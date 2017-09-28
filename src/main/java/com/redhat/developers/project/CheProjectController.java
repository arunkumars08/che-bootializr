package com.redhat.developers.project;

import com.redhat.developers.service.TemplateService;
import freemarker.template.TemplateException;
import io.spring.initializr.generator.BasicProjectRequest;
import io.spring.initializr.generator.ProjectGenerator;
import io.spring.initializr.generator.ProjectRequest;
import io.spring.initializr.metadata.DependencyMetadataProvider;
import io.spring.initializr.metadata.InitializrMetadataProvider;
import io.spring.initializr.util.TemplateRenderer;
import io.spring.initializr.web.project.MainController;
import lombok.extern.slf4j.Slf4j;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ZipFileSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class CheProjectController extends MainController {


    private static final String DEFAULT_SPRING_BOOT_CHE_IMAGE = "rhche/spring-boot";

    @Value("${git.userid}")
    private String githubUserId;

    private final ProjectGenerator projectGenerator;

    private final TemplateService templateService;

    public CheProjectController(InitializrMetadataProvider metadataProvider, TemplateRenderer templateRenderer,
                                ResourceUrlProvider resourceUrlProvider, ProjectGenerator projectGenerator,
                                DependencyMetadataProvider dependencyMetadataProvider, TemplateService templateService) {
        super(metadataProvider, templateRenderer, resourceUrlProvider, projectGenerator, dependencyMetadataProvider);
        this.projectGenerator = projectGenerator;
        this.templateService = templateService;
    }

    @RequestMapping(value = "/", produces = "text/html")
    public String home(Map<String, Object> model) {
        super.home(model);
        return "newhome";
    }

    @RequestMapping("/chestarter.zip")
    @ResponseBody
    public ResponseEntity<byte[]> cheSpringZip(BasicProjectRequest request) {
        try {

            log.info("Creating Project with Che-Starter");

            Map<String, String> projectContext = new HashMap<>();

            String projectDescription = request.getDescription();
            String projectArtifactId = request.getArtifactId();

            projectContext.put("artifactId", projectArtifactId);
            projectContext.put("description", projectDescription);
            //FIXME
            projectContext.put("githubRepoUrl", String.format("https://github.com/%s/%s", githubUserId, projectArtifactId));
            projectContext.put("dockerImage", DEFAULT_SPRING_BOOT_CHE_IMAGE);


            //Step-2 Generate Project as zip with Che .factory.json.ftl
            String factoryJson = templateService.buildFactoryJsonFromTemplate(projectContext);

            log.trace("factoryJson:{}", factoryJson);

            ProjectRequest projectRequest = (ProjectRequest) request;

            File projectDir = projectGenerator.generateProjectStructure(projectRequest);

            File projectDistribution = projectGenerator.createDistributionFile(projectDir, ".zip");

            String wrapperScript = request.getBaseDir() != null
                    ? request.getBaseDir() + "/mvnw" : "mvnw";
            new File(projectDir, wrapperScript).setExecutable(true);


            String factoryJsonFile = request.getBaseDir() != null
                    ? request.getBaseDir() + "/.factory.json" : ".factory.json";

            //Write Che Factory Json File
            Files.write(Paths.get(projectDir.getAbsolutePath(), factoryJsonFile), factoryJson.getBytes());

            Zip projectZip = new Zip();
            projectZip.setProject(new Project());

            ZipFileSet zipFileSet = new ZipFileSet();
            zipFileSet.setDir(projectDir);
            zipFileSet.setFileMode("755");
            zipFileSet.setIncludes("**,");
            zipFileSet.setExcludes(wrapperScript);
            zipFileSet.setDefaultexcludes(false);
            projectZip.addFileset(zipFileSet);
            projectZip.setDestFile(projectDistribution.getCanonicalFile());
            projectZip.execute();

            //FIXME: Step-2 Create Project on Github and Push the contents


            //Build Response

            String downloadFileName = sanitizedUrlEncodedName(projectArtifactId, "zip");
            String contentDispositionHeaderValue = "attachment; filename=\"" + downloadFileName + "\"";

            byte[] bytes = StreamUtils.copyToByteArray(new FileInputStream(projectDistribution));

            projectGenerator.cleanTempFiles(projectDir);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/zip")
                    .header("Content-Disposition", contentDispositionHeaderValue)
                    .body(bytes);

        } catch (IOException e) {
            log.error("Error:", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (TemplateException e) {
            log.error("Error:", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error:", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param projectArtifactId
     * @param extension
     * @return
     */
    private String sanitizedUrlEncodedName(String projectArtifactId, String extension) {
        String baseName = StringUtils.replace(projectArtifactId, " ", "_");
        try {
            return URLEncoder.encode(baseName, "UTF-8") + "." + extension;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
