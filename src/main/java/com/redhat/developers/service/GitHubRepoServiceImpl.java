/**
 * Copyright (c) 2017 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.developers.service;

import com.redhat.developers.config.CheBootalizrProperties;
import com.redhat.developers.vo.RepoVO;
import org.apache.commons.lang.text.StrSubstitutor;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

@Service
@Slf4j
public class GitHubRepoServiceImpl implements RepoService {

    private final CheBootalizrProperties cheBootalizrProperties;

    private String githubUserId;
    private String githubUserToken;

    public GitHubRepoServiceImpl(CheBootalizrProperties cheBootalizrProperties) {
        this.cheBootalizrProperties = cheBootalizrProperties;
    }

    @PostConstruct
    public void init() throws IOException, URISyntaxException {

        String githubUserNameFilePath = StrSubstitutor.replaceSystemProperties("${user.home}/github/user");
        String githubUserTokenFilePath = StrSubstitutor.replaceSystemProperties("${user.home}/github/token");

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
            }
        } catch (NoSuchFileException e) {
            log.warn(e.getMessage());
        } catch (IOException e) {
            throw e;
        } catch (URISyntaxException e) {
            throw e;
        }
    }

    @Override
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

            ghRepo = buildAndReturnRepoVO(ghRepository);

        } else {
            log.info("Repository {} already exists, skipping creation", ghRepo.get().getName());
        }

        return ghRepo;
    }

    @Override
    public Optional<RepoVO> getRepo(String repoName) throws IOException {

        try {
            final GitHub gitHub = getConnection();
            log.info("Retrieving repository {} with GitHub user {}", repoName, githubUserId);
            GHRepository ghRepository = gitHub.getRepository(String.format("%s/%s", githubUserId, repoName));
            return buildAndReturnRepoVO(ghRepository);
        } catch (GHFileNotFoundException e) {
            log.info("Repository {} does not exists, skipping delete", repoName);
            return Optional.ofNullable(null);
        } catch (IOException e) {
            log.error("Unable to get repo ", e);
            throw e;
        }
    }


    @Override
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

    private Optional<RepoVO> buildAndReturnRepoVO(GHRepository ghRepository) {
        if (ghRepository != null) {
            return Optional.of(RepoVO.builder()
                .name(ghRepository.getName())
                .description(ghRepository.getDescription())
                .httpUrl(ghRepository.gitHttpTransportUrl())
                .url(ghRepository.getGitTransportUrl())
                .sshUrl(ghRepository.getSshUrl())
                .fqdn(ghRepository.getFullName())
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
