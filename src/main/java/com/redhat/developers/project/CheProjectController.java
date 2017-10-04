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
import com.redhat.developers.vo.ProjectMissionVO;
import com.redhat.developers.vo.RepoVO;
import io.openshift.booster.catalog.Booster;
import io.openshift.booster.catalog.BoosterCatalogService;
import io.openshift.booster.catalog.Mission;
import io.openshift.booster.catalog.Runtime;
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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;

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

    public CheProjectController(InitializrMetadataProvider metadataProvider,
                                DependencyMetadataProvider dependencyMetadataProvider,
                                ProjectGenerator projectGenerator, TemplateService templateService,
                                GitHubRepoService gitHubRepoService,
                                CheBootalizrProperties cheBootalizrProperties,
                                BoosterCatalogService boosterCatalogService) {
        this.projectGenerator = projectGenerator;
        this.templateService = templateService;
        this.gitHubRepoService = gitHubRepoService;
        this.metadataProvider = metadataProvider;
        this.dependencyMetadataProvider = dependencyMetadataProvider;
        this.cheBootalizrProperties = cheBootalizrProperties;
        this.boosterCatalogService = boosterCatalogService;
    }

    @PostConstruct
    public void init() {
        try {
            boosterCatalogService.index().get();
        } catch (InterruptedException e) {
            log.error("Error loading missions:", e);
        } catch (ExecutionException e) {
            log.error("Error loading missions:", e);
        }
    }

    @ModelAttribute
    public BasicProjectRequest projectRequest(@RequestHeader Map<String, String> headers) {
        ProjectRequest projectRequest = new ProjectRequest();
        projectRequest.getParameters().putAll(headers);
        projectRequest.initialize(metadataProvider.get());
        return projectRequest;
    }

    @ModelAttribute("projectMissions")
    public List<ProjectMissionVO> projectMissions() {
        List<ProjectMissionVO> projectMissions = new LinkedList<>();
        Set<Mission> missions = boosterCatalogService.getMissions();
        log.info("Total Missions:{}", missions.size());
        missions.forEach(mission -> {
            ProjectMissionVO missionVO = new ProjectMissionVO();
            missionVO.setMission(mission);
            Optional<Booster> optBooster = boosterCatalogService.getBooster(mission, new Runtime("spring-boot"));
            if (optBooster.isPresent()) {
                Booster booster = optBooster.get();
                log.trace("Adding Booster :" + booster.getName());
                missionVO.setBooster(optBooster.get());
                missionVO.setBoosterDescription(
                    GeneralUtil.descriptionToString(booster.getDescription()));
            }
            projectMissions.add(missionVO);
        });
        return projectMissions;
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

    @RequestMapping(value = "/dummy")
    @ResponseBody
    public ResponseEntity<byte[]> dummy(BasicProjectRequest request, String projectMission) {

        log.info("DUMMY PROCESSOR: Project Req:{} with Project Mission: {}", request.getArtifactId(),
            projectMission);

        String[] temp = projectMission.split("~");
        String missionId = temp[0];
        String boosterId = temp[1];

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
                Model springInitalizrPomModel = mavenXpp3Reader.read(new StringReader(springPom));

                File boosterPomFile = Paths.get(projectRootDir.toFile().getAbsolutePath(),
                    "pom.xml").toFile();

                Model boosterPomModel = mavenXpp3Reader.read(new FileReader(boosterPomFile));

                mergeModel(springInitalizrPomModel, boosterPomModel);

                //Write back the pom
                MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();

                Files.delete(Paths.get(boosterPomFile.getAbsolutePath()));


                mavenXpp3Writer.write(new FileWriter(boosterPomFile),
                    boosterPomModel);

                File downloadFile = projectGenerator.createDistributionFile(projectDir.toFile(), ".zip");

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
                String contentDispositionValue = "attachment; filename=\"" + sanitzedUrlFileName(projectRequest, "zip") + "\"";

                return ResponseEntity.ok().header("Content-Type", "application/zip")
                    .header("Content-Disposition", contentDispositionValue).body(bytes);

            } catch (IOException e) {
                log.error("Error creating project ", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage().getBytes());
            } catch (XmlPullParserException e) {

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage().getBytes());
            }
        } else {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(new byte[0]);
        }
    }

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

        if (dependencyManagement != null) {
            DependencyManagement boosterDepMgmt = boosterPomModel.getDependencyManagement();
            if (dependencyManagement.getDependencies() != null) {
                dependencyManagement.getDependencies().forEach(dependency ->
                    boosterDepMgmt.getDependencies().add(dependency));
            }
        }

        //Dependencies
        List<Dependency> springDeps = springInitalizrPomModel.getDependencies();
        List<Dependency> boosterDeps = boosterPomModel.getDependencies();

        if ((boosterDeps != null && !!boosterDeps.isEmpty()) &&
            (springDeps != null && !!springDeps.isEmpty())) {
            boosterDeps.addAll(boosterDeps);
        }

    }

    @RequestMapping(value = "/cheproject")
    public ModelAndView springbootCheProject(BasicProjectRequest request, ModelAndView modelAndView) {
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

                    ProjectRequest projectRequest = (ProjectRequest) request;

                    File projectDir = projectGenerator.generateProjectStructure(projectRequest);

                    log.trace("Project created at : {}", projectDir);

                    String wrapperScript = request.getBaseDir() != null
                        ? request.getBaseDir() + "/mvnw" : "mvnw";
                    new File(projectDir, wrapperScript).setExecutable(true);


                    String factoryJsonFile = request.getBaseDir() != null
                        ? request.getBaseDir() + "/.factory.json" : ".factory.json";

                    //Write Che Factory Json File
                    Files.write(Paths.get(projectDir.getAbsolutePath(), factoryJsonFile), factoryJson.getBytes());

                    gitHubRepoService.pushContentToOrigin(githubRepo.getName(),
                        Paths.get(projectDir.getAbsolutePath(),
                            request.getBaseDir() != null ? request.getBaseDir() : "/").toFile(), githubRepo);

                } else { //add .factory.json to repos that might not have it
                    gitHubRepoService.addFactoryJsonIfMissing(factoryJson, repoName, githubRepo);
                }

                if (githubRepo.isCreated()) {
                    log.info("Auto Redirect will happen");
                } else {
                    log.info("Auto Redirect not happen, link will be provided");
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
     * @param repoVO
     * @param artifactId
     * @param groupId
     * @param name
     * @param packaging
     * @return
     */
    protected CheSpringBootProjectVO buildResponse(RepoVO repoVO, String artifactId, String groupId, String name, String packaging) {
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


    private static String sanitzedUrlFileName(ProjectRequest request, String extension) {
        String tmp = request.getArtifactId().replaceAll(" ", "_");
        try {
            return URLEncoder.encode(tmp, "UTF-8") + "." + extension;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Cannot encode URL", e);
        }
    }

}
