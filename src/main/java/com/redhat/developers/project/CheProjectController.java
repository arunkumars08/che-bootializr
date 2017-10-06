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
package com.redhat.developers.project;

import com.redhat.developers.config.CheBootalizrProperties;
import com.redhat.developers.service.GitHubRepoService;
import com.redhat.developers.service.TemplateService;
import com.redhat.developers.utils.GeneralUtil;
import com.redhat.developers.vo.CheSpringBootProjectVO;
import com.redhat.developers.vo.RepoVO;
import io.openshift.booster.catalog.Booster;
import io.openshift.booster.catalog.BoosterCatalogService;
import io.spring.initializr.generator.BasicProjectRequest;
import io.spring.initializr.generator.ProjectGenerator;
import io.spring.initializr.generator.ProjectRequest;
import io.spring.initializr.metadata.DependencyMetadataProvider;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ZipFileSet;
import org.asciidoctor.Asciidoctor;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.beans.PropertyDescriptor;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author kameshsampath
 */
@Controller
@Slf4j
public class CheProjectController {


    private static final String DEFAULT_SPRING_BOOT_CHE_IMAGE = "rhche/spring-boot";

    private final ProjectGenerator projectGenerator;

    private final TemplateService templateService;
    private final GitHubRepoService gitHubRepoService;
    private final InitializrMetadataProvider metadataProvider;
    private final DependencyMetadataProvider dependencyMetadataProvider;
    private final BoosterCatalogService boosterCatalogService;
    private final CheBootalizrProperties cheBootalizrProperties;
    private final Asciidoctor asciidoctor;

    public CheProjectController(InitializrMetadataProvider metadataProvider,
                                DependencyMetadataProvider dependencyMetadataProvider,
                                ProjectGenerator projectGenerator, TemplateService templateService,
                                GitHubRepoService gitHubRepoService,
                                CheBootalizrProperties cheBootalizrProperties,
                                BoosterCatalogService boosterCatalogService, Asciidoctor asciidoctor) {
        this.projectGenerator = projectGenerator;
        this.templateService = templateService;
        this.gitHubRepoService = gitHubRepoService;
        this.metadataProvider = metadataProvider;
        this.dependencyMetadataProvider = dependencyMetadataProvider;
        this.cheBootalizrProperties = cheBootalizrProperties;
        this.boosterCatalogService = boosterCatalogService;
        this.asciidoctor = asciidoctor;
    }

    @ModelAttribute
    public BasicProjectRequest projectRequest(@RequestHeader Map<String, String> headers) {
        ProjectRequest projectRequest = new ProjectRequest();
        projectRequest.getParameters().putAll(headers);
        projectRequest.initialize(metadataProvider.get());
        return projectRequest;
    }

    @GetMapping("/")
    public String home(Map<String, Object> model) {
        InitializrMetadata metadata = metadataProvider.get();
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(metadata);
        for (PropertyDescriptor descriptor : beanWrapper.getPropertyDescriptors()) {
            model.put(descriptor.getName(), beanWrapper.getPropertyValue(descriptor.getName()));
        }
        model.put("defaultAction", "cheproject");
        return "home";
    }

    /**
     * @param request
     * @param projectBooster
     * @return
     */
    @RequestMapping(value = "/cheproject.zip")
    @ResponseBody
    public ResponseEntity<byte[]> cheprojectAsZip(BasicProjectRequest request, String projectBooster) {

        try {

            ProjectRequest projectRequest = (ProjectRequest) request;
            Path projectDir = makeProject(request, projectBooster);

            File downloadFile = projectGenerator.createDistributionFile(projectDir.toFile(), ".zip");

            return makeDownload(projectRequest, projectDir, downloadFile);

        } catch (IOException e) {
            log.error("Error creating project ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage().getBytes());
        } catch (XmlPullParserException e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage().getBytes());
        }
    }


    /**
     * @param request
     * @param modelAndView
     * @return
     */
    @RequestMapping(value = "/cheproject")
    public ModelAndView springbootCheProject(BasicProjectRequest request, String projectBooster,
                                             ModelAndView modelAndView) {
        try {

            final String projectDescription = request.getDescription();
            final String projectArtifactId = request.getArtifactId();
            final String projectGroupId = request.getGroupId();
            final String projectName = request.getName();
            final String projectPackaging = request.getPackaging();

            String repoName = StringUtils.replace(projectArtifactId, " ", "_");
            Optional<RepoVO> optRepoVo = gitHubRepoService.createRepo(repoName, projectDescription);

            if (optRepoVo.isPresent()) {

                RepoVO githubRepo = optRepoVo.get();

                final Map<String, String> projectContext = new HashMap<>();
                projectContext.put("artifactId", projectArtifactId);
                projectContext.put("description", projectDescription);
                projectContext.put("githubRepoUrl", githubRepo.getHttpUrl());
                projectContext.put("dockerImage", DEFAULT_SPRING_BOOT_CHE_IMAGE);

                String factoryJson = templateService.buildFactoryJsonFromTemplate(projectContext);

                //Only Created repos need code push, existing repositories does not need it
                if (githubRepo.isCreated()) {

                    Path projectDir = makeProject(request, projectBooster);
                    File fProjectDir = projectDir.toFile();

                    log.trace("Project created at : {}", projectDir);

                    //Write Che Factory Json File
                    Files.write(Paths.get(fProjectDir.getAbsolutePath(), "/.factory.json"), factoryJson.getBytes());

                    gitHubRepoService.pushContentToOrigin(githubRepo.getName(),
                        Paths.get(fProjectDir.getAbsolutePath()).toFile(), githubRepo);

                } else { //add .factory.json to repos that might not have it
                    gitHubRepoService.addFactoryJsonIfMissing(factoryJson, repoName, githubRepo);
                }

                CheSpringBootProjectVO response = buildResponse(githubRepo, projectArtifactId, projectGroupId,
                    projectName, projectPackaging);

                modelAndView.getModel().put("projectInfo", response);
                modelAndView.setViewName("ok");

            } else {
                log.info("Repo not present, hence skipping creation");
                modelAndView.setViewName("error");
            }

        } catch (IOException e) {
            log.error("Error:", e);
            modelAndView.setViewName("error");
        } catch (Exception e) {
            log.error("Error:", e);
            modelAndView.setViewName("error");
        }

        return modelAndView;
    }

    /**
     * @param request
     * @param boosterId
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */

    protected Path makeProject(BasicProjectRequest request, String boosterId)
        throws IOException, XmlPullParserException {

        if (boosterId != null) {

            Optional<Booster> optional = boosterCatalogService.getBoosters().stream()
                .filter(b -> b.getId().equals(boosterId))
                .findFirst();

            if (optional.isPresent()) {

                Booster booster = optional.get();
                ProjectRequest projectRequest = (ProjectRequest) request;
                byte[] springPomBytes = projectGenerator.generateMavenPom(projectRequest);
                String springPom = new String(springPomBytes);

                Path projectDir;

                try {

                    projectDir = Files.createTempDirectory("tmpchebootializr");
                    log.info("Project temp directory :{}", projectDir.toFile().getAbsolutePath());
                    Files.deleteIfExists(projectDir);
                    projectDir.toFile().mkdirs();
                    Path projectRootDir;
                    if (request.getBaseDir() != null) {
                        projectRootDir = Files.createDirectories(Paths.get(projectDir.toFile().getAbsolutePath(), request.getBaseDir()));
                    } else {
                        projectRootDir = projectDir;
                    }
                    boosterCatalogService.copy(booster, projectRootDir);

                    log.info("Booster copied to  Dir: {}", projectRootDir);

                    MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();

                    Model springInitalizrPomModel;

                    try (StringReader stringPomReader = new StringReader(springPom)) {
                        springInitalizrPomModel = mavenXpp3Reader.read(stringPomReader);
                    }

                    File boosterPomFile = Paths.get(projectRootDir.toFile().getAbsolutePath(),
                        "pom.xml").toFile();

                    Model boosterPomModel;
                    try (FileReader pomReader = new FileReader(boosterPomFile)) {
                        boosterPomModel = mavenXpp3Reader.read(pomReader);
                    }

                    mergeModel(springInitalizrPomModel, boosterPomModel);

                    //Write back the pom
                    MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
                    Files.delete(Paths.get(boosterPomFile.getAbsolutePath()));
                    try (FileWriter pomWriter = new FileWriter(boosterPomFile)) {
                        mavenXpp3Writer.write(pomWriter, boosterPomModel);
                    }

                    return projectRootDir;

                } catch (IOException e) {
                    log.error("Error creating project ", e);
                    throw e;
                } catch (XmlPullParserException e) {
                    log.error("Error creating project ", e);
                    throw e;
                }
            } else {
                throw new IllegalStateException("Unable to build project");
            }
        } else {
            return makeProject(request);
        }

    }

    /**
     * @param request
     * @return
     * @throws Exception
     */
    private Path makeProject(BasicProjectRequest request) {
        ProjectRequest projectRequest = (ProjectRequest) request;

        File projectDir = projectGenerator.generateProjectStructure(projectRequest);

        log.trace("Project created at : {}", projectDir);

        return Paths.get(projectDir.toURI());
    }

    /**
     * @param repoVO
     * @param artifactId
     * @param groupId
     * @param name
     * @param packaging
     * @return
     */

    private CheSpringBootProjectVO buildResponse(RepoVO repoVO, String artifactId, String groupId, String name, String packaging) {
        String workspaceUrl = String.format("%s/f?url=%s", cheBootalizrProperties.getChe().getServiceUrl()
            , GeneralUtil.sanitizeGitUrl(repoVO.getHttpUrl()));

        CheSpringBootProjectVO cheSpringBootProjectVO = CheSpringBootProjectVO.builder()
            .artifactId(artifactId)
            .groupId(groupId)
            .name(name)
            .packaging(packaging)
            .created(repoVO.isCreated())
            .workspaceUrl(workspaceUrl).build();

        return cheSpringBootProjectVO;
    }


    /**
     * @param projectRequest
     * @param projectDir
     * @param downloadFile
     * @return
     * @throws IOException
     */
    private ResponseEntity<byte[]> makeDownload(ProjectRequest projectRequest, Path projectDir, File downloadFile) throws IOException {
        Zip zip = new Zip();
        zip.setProject(new Project());
        zip.setDefaultexcludes(false);
        ZipFileSet set = new ZipFileSet();
        set.setDir(projectDir.toFile());
        set.setIncludes("**,");
        set.setDefaultexcludes(false);
        zip.addFileset(set);
        zip.setDestFile(downloadFile.getCanonicalFile());
        zip.execute();

        //send Response Entity
        byte[] bytes = StreamUtils.copyToByteArray(new FileInputStream(downloadFile));
        String contentDispositionValue = "attachment; filename=\"" + GeneralUtil.sanitizedUrlEncodedName(projectRequest, "zip") + "\"";

        return ResponseEntity.ok().header("Content-Type", "application/zip")
            .header("Content-Disposition", contentDispositionValue).body(bytes);
    }


    /**
     * @param springInitalizrPomModel
     * @param boosterPomModel
     */
    private void mergeModel(Model springInitalizrPomModel, Model boosterPomModel) {

        log.info("Merging Booster POM with Spring Intializr Pom");

        //GAV
        boosterPomModel.setGroupId(springInitalizrPomModel.getGroupId());
        boosterPomModel.setArtifactId(springInitalizrPomModel.getArtifactId());
        boosterPomModel.setVersion(springInitalizrPomModel.getVersion());

        if (!"demo".equalsIgnoreCase(springInitalizrPomModel.getName())) {
            boosterPomModel.setName(springInitalizrPomModel.getName());
        }
        if (!"Demo project for Spring Boot".equalsIgnoreCase(springInitalizrPomModel.getName())) {
            boosterPomModel.setDescription(springInitalizrPomModel.getName());
        }

        boosterPomModel.setVersion(springInitalizrPomModel.getDescription());

        //Properties
        Properties boosterPomProps = boosterPomModel.getProperties();

        springInitalizrPomModel.getProperties().forEach((key, value) -> {
            boosterPomProps.putIfAbsent(key, value);
        });

        //Dependency Management
        DependencyManagement dependencyManagement = springInitalizrPomModel.getDependencyManagement();
        DependencyManagement boosterDepMgmt = boosterPomModel.getDependencyManagement();
        if (dependencyManagement != null && boosterDepMgmt != null) {
            if (dependencyManagement.getDependencies() != null) {
                dependencyManagement.getDependencies().forEach(dependency ->
                    boosterDepMgmt.getDependencies().add(dependency));
            }
        }

        //Dependencies
        List<Dependency> springDeps = springInitalizrPomModel.getDependencies();
        List<Dependency> boosterDeps = boosterPomModel.getDependencies();

        if ((boosterDeps != null && !boosterDeps.isEmpty()) &&
            (springDeps != null && !springDeps.isEmpty())) {
            if ("pom".equals(boosterPomModel.getPackaging())) {
                if (boosterDepMgmt != null) {
                    Collection<Dependency> merged = distinctDependencies(boosterDepMgmt.getDependencies(), springDeps);
                    boosterDepMgmt.getDependencies().clear();
                    boosterDepMgmt.getDependencies().addAll(merged);
                }
            } else {
                Collection<Dependency> merged = distinctDependencies(boosterDeps, springDeps);
                boosterDeps.clear();
                boosterDeps.addAll(merged);
            }
        }

        log.info("Successfully merged Booster POM with Spring Intializr Pom");
    }

    private Collection<Dependency> distinctDependencies(List<Dependency> boosterDeps, List<Dependency> springDeps) {

        Comparator<Dependency> scopeComparator = Comparator.comparing(Dependency::getScope,
            Comparator.nullsLast(Comparator.naturalOrder()));

        Set<Dependency> uniqueDeps = new TreeSet<>(Comparator.comparing(Dependency::getGroupId)
            .thenComparing(Dependency::getArtifactId).thenComparing(scopeComparator));

        uniqueDeps.addAll(boosterDeps);
        uniqueDeps.addAll(springDeps);

        return uniqueDeps;
    }

}
