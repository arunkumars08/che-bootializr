/**
 * Copyright (c) 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.developers.service;

import com.redhat.developers.config.CheBootalizrProperties;
import com.redhat.developers.vo.RepoVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.text.StrSubstitutor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

@Service
@Slf4j
public class GitHubRepoService {

    private final CheBootalizrProperties cheBootalizrProperties;

    private String githubUserId;
    private String githubUserToken;

    public GitHubRepoService(CheBootalizrProperties cheBootalizrProperties) {
        this.cheBootalizrProperties = cheBootalizrProperties;
    }

    @PostConstruct
    public void init() throws IOException, URISyntaxException {

        String githubUserNameFilePath = cheBootalizrProperties.getChe().getGithubUserFile();
        String githubUserTokenFilePath = cheBootalizrProperties.getChe().getGithubUserTokenFile();

        try {
            URL githubUserSecretsURL = ResourceUtils.getURL(githubUserNameFilePath);
            URL githubUserTokenSecretsURL = ResourceUtils.getURL(githubUserTokenFilePath);

            if (githubUserSecretsURL != null && githubUserTokenSecretsURL != null) {

                URI githubUserSecretsURI = githubUserSecretsURL.toURI();
                URI githubUserTokenSecretsURI = githubUserTokenSecretsURL.toURI();

                final byte[] encodedGithubUser = Files.readAllBytes(Paths.get(githubUserSecretsURI));
                final byte[] encodedGithubToken = Files.readAllBytes(Paths.get(githubUserTokenSecretsURI));

                this.githubUserId = sanitize(encodedGithubUser);

                this.githubUserToken = sanitize(encodedGithubToken);

                log.info("Git User and Password set from user and token file");
            }
        } catch (NoSuchFileException e) {
            log.warn(e.getMessage());
        } catch (IOException e) {
            throw e;
        } catch (URISyntaxException e) {
            throw e;
        }
    }

    public Optional<RepoVO> createRepo(String repoName, String description) throws IOException {

        final GitHub gitHub = getConnection();

        log.info("Creating repository {} with GitHub user {}", repoName, githubUserId);

        Optional<RepoVO> ghRepo = getRepo(repoName);

        if (!ghRepo.isPresent()) {
            GHRepository ghRepository =
                gitHub.createRepository(Objects.requireNonNull(repoName, "GitHub repo name cant be null"))
                    .description(description)
                    .create();

            log.info("Successfully Created repository {}, push URL {}",
                ghRepository.getName(), ghRepository.getGitTransportUrl());

            ghRepo = buildAndReturnRepoVO(ghRepository, true);

        } else {
            log.info("Repository {} already exists, skipping creation", ghRepo.get().getName());
        }

        return ghRepo;
    }

    public Optional<RepoVO> getRepo(String repoName) throws IOException {

        try {
            final GitHub gitHub = getConnection();
            log.info("Retrieving repository {} with GitHub user {}", repoName, githubUserId);
            GHRepository ghRepository = gitHub.getRepository(String.format("%s/%s", githubUserId, repoName));
            return buildAndReturnRepoVO(ghRepository, false);
        } catch (GHFileNotFoundException e) {
            log.info("Repository {} does not exists", repoName);
            return Optional.ofNullable(null);
        } catch (IOException e) {
            log.error("Unable to get repo ", e);
            throw e;
        }
    }

    public boolean deleteRepo(String repoName) throws IOException {

        try {
            log.info("Deleting repository {} with GitHub user {}", repoName, githubUserId);
            final GitHub gitHub = getConnection();
            GHRepository ghRepository = gitHub.getRepository(String.format("%s/%s", githubUserId, repoName));
            log.info("Repository exists, deleting it");
            ghRepository.delete();
            return true;
        } catch (GHFileNotFoundException e) {
            log.info("Repository {} does not exists, skipping delete", repoName);
            return false;
        } catch (IOException e) {
            throw e;
        }

    }

    /**
     *
     * @param repoName
     * @param dir
     * @param repoVO
     * @throws IOException
     * @throws URISyntaxException
     * @throws GitAPIException
     */
    public void pushContentToOrigin(String repoName, File dir, RepoVO repoVO)
        throws IOException, URISyntaxException, GitAPIException {
        try (Git git = Git.init()
            .setDirectory(dir)
            .call()) {

            Repository repository = git.getRepository();

            //Set Remote
            final StoredConfig config = repository.getConfig();
            RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
            final URIish uri = new URIish(repository.getDirectory().toURI().toURL());
            final URIish remotePushURI = new URIish(repoVO.getHttpUrl());
            remoteConfig.addURI(uri);
            remoteConfig.addPushURI(remotePushURI);
            remoteConfig.update(config);
            config.save();

            //Add files to index
            git.add().addFilepattern(".").call();

            //Create commit
            RevCommit commit = git.commit().setMessage("Initial Commit by CheBootalizr").call();
            RefSpec refSpec = new RefSpec("refs/heads/master");
            git.push()
                .setRemote("origin")
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubUserId, githubUserToken))
                .setRefSpecs(refSpec)
                .call();

            Objects.equals(commit.getId(), repository.resolve(commit.getId().getName() + "^{commit}"));
        }

    }

    /**
     * @param factoryJson
     * @param repoName
     * @param repoVO
     * @throws IOException
     * @throws GitAPIException
     */
    public void addFactoryJsonIfMissing(String factoryJson, String repoName, RepoVO repoVO) throws IOException, GitAPIException {

        Path repoDir = Files.createTempDirectory(repoName);

        String REMOTE_URL = repoVO.getHttpUrl();

        try (Git git = Git.cloneRepository()
            .setURI(REMOTE_URL)
            .setDirectory(repoDir.toFile())
            .call()) {

            log.trace("Repo location : " + git.getRepository().getDirectory());

            Repository repository = git.getRepository();

            Optional<Path> factoryJsonExist = Files.list(Paths.get(repository.getDirectory().getParent()))
                .filter(path -> path.endsWith(".factory.json"))
                .findFirst();

            if (factoryJsonExist.isPresent()) {
                log.info("Factory JSON present in repo '{}' nothing todo", repoVO.getHttpUrl());
            } else {
                log.info("Factory JSON not present in repo '{}' will add and push", repoVO.getHttpUrl());
                File jsonFile =
                    new File(repository.getDirectory().getParent(), ".factory.json");

                jsonFile.createNewFile();
                Files.write(Paths.get(jsonFile.toURI()), factoryJson.getBytes());

                git.add().addFilepattern(".factory.json").call();

                git.commit()
                    .setMessage("Added Che factory config file")
                    .call();

                git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubUserId, githubUserToken))
                    .call();
            }

        }

    }

    /**
     * @param ghRepository
     * @param isCreated
     * @return
     */
    private Optional<RepoVO> buildAndReturnRepoVO(GHRepository ghRepository, boolean isCreated) {
        if (ghRepository != null) {
            return Optional.of(RepoVO.builder()
                .name(ghRepository.getName())
                .description(ghRepository.getDescription())
                .httpUrl(ghRepository.gitHttpTransportUrl())
                .url(ghRepository.getGitTransportUrl())
                .sshUrl(ghRepository.getSshUrl())
                .fqdn(ghRepository.getFullName())
                .created(isCreated)
                .build());
        } else {
            return Optional.empty();
        }
    }

    /**
     * @return
     * @throws IOException
     */
    private GitHub getConnection() throws IOException {

        if (githubUserId != null && githubUserToken != null) {
            return GitHub
                .connect(githubUserId, githubUserToken);
        }
        //try to load from file ~/.github which is default
        if (githubUserId == null) {
            Properties githubProperties = new Properties();
            String githubProps = StrSubstitutor.replaceSystemProperties("${user.home}/.github");
            githubProperties.load(ResourceUtils.getURL(githubProps).openStream());
            githubUserId = githubProperties.getProperty("login");
            githubUserToken = githubProperties.getProperty("oauth");
            log.info("Loaded GitHub credentials for user {} from ~/.github", githubUserId);
        }
        return GitHub.connect();
    }

    /**
     * remove all new lines from the String
     *
     * @param strBytes - the string bytes where newline to be removed
     * @return sanitized string without newlines
     */
    private String sanitize(byte[] strBytes) {
        return new String(strBytes)
            .replace("\r", "")
            .replace("\n", "");
    }


}
