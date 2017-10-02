package com.redhat.developers.project;

import com.redhat.developers.config.CheBootalizrProperties;
import com.redhat.developers.service.GitHubRepoService;
import com.redhat.developers.service.TemplateService;
import com.redhat.developers.utils.GeneralUtil;
import com.redhat.developers.vo.CheSpringBootProjectVO;
import com.redhat.developers.vo.RepoVO;
import io.spring.initializr.generator.BasicProjectRequest;
import io.spring.initializr.generator.ProjectGenerator;
import io.spring.initializr.generator.ProjectRequest;
import io.spring.initializr.metadata.DependencyMetadataProvider;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@Slf4j
public class CheProjectController {


    private static final String DEFAULT_SPRING_BOOT_CHE_IMAGE = "rhche/spring-boot";

    private final ProjectGenerator projectGenerator;

    private final TemplateService templateService;
    private final GitHubRepoService gitHubRepoService;
    private final InitializrMetadataProvider metadataProvider;
    private final DependencyMetadataProvider dependencyMetadataProvider;
    private final CheBootalizrProperties cheBootalizrProperties;

    public CheProjectController(InitializrMetadataProvider metadataProvider,
                                DependencyMetadataProvider dependencyMetadataProvider,
                                ProjectGenerator projectGenerator, TemplateService templateService,
                                GitHubRepoService gitHubRepoService, CheBootalizrProperties cheBootalizrProperties) {
        this.projectGenerator = projectGenerator;
        this.templateService = templateService;
        this.gitHubRepoService = gitHubRepoService;
        this.metadataProvider = metadataProvider;
        this.dependencyMetadataProvider = dependencyMetadataProvider;
        this.cheBootalizrProperties = cheBootalizrProperties;

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


    @RequestMapping(value = "/cheproject")
    public ModelAndView springbootCheProject(BasicProjectRequest request, ModelAndView modelAndView) {
        try {

            log.info("Creating Project with Che-Starter");

            Map<String, String> projectContext = new HashMap<>();

            final String projectDescription = request.getDescription();
            final String projectArtifactId = request.getArtifactId();
            final String projectGroupId = request.getGroupId();
            final String projectName = request.getName();
            final String projectPackaging = request.getPackaging();

            Optional<RepoVO> optRepoVo = gitHubRepoService.createRepo(projectArtifactId, projectDescription);

            //FIXME if repo exists already then just return URL with existing repo

            if (optRepoVo.isPresent()) {

                projectContext.put("artifactId", projectArtifactId);
                projectContext.put("description", projectDescription);
                projectContext.put("githubRepoUrl", optRepoVo.get().getHttpUrl());
                projectContext.put("dockerImage", DEFAULT_SPRING_BOOT_CHE_IMAGE);

                //Step-2 Generate Project as zip with Che .factory.json
                String factoryJson = templateService.buildFactoryJsonFromTemplate(projectContext);

                log.trace("factoryJson:{}", factoryJson);

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

                // Step-2  Push the contents to GitHub
                String repoName = StringUtils.replace(projectArtifactId, " ", "_");
                Optional<RepoVO> gitHubRepo = gitHubRepoService.createRepo(repoName, projectDescription);

                if (gitHubRepo.isPresent()) {

                    gitHubRepoService.pushContentToOrigin(gitHubRepo.get().getName(),
                        Paths.get(projectDir.getAbsolutePath(),
                            request.getBaseDir() != null ? request.getBaseDir() : "/").toFile());

                    CheSpringBootProjectVO response = buildResponse(gitHubRepo.get(), projectArtifactId, projectGroupId,
                        projectName, projectPackaging);

                    modelAndView.getModel().put("projectInfo", response);

                }

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
            .workspaceUrl(workspaceUrl).build();

        return cheSpringBootProjectVO;
    }


}
