package com.redhat.developers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.redhat.developers.service.TemplateService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FactoryJsonTest {

    @Autowired
    TemplateService templateService;

    @Test
    public void should_build_template_with_project_details() throws Exception {
        assertThat(templateService).isNotNull();

        Map<String, String> context = new HashMap<>();
        context.put("artifactId", "demo-project");
        context.put("description", "My Description");
        context.put("githubRepoUrl", "https://github.com/demo-project");
        context.put("dockerImage", "rhche/spring-boot");

        String factoryJson = templateService.buildFactoryJsonFromTemplate(context);

        assertThat(factoryJson).isNotNull();

        String dockerImage = JsonPath.read(factoryJson, "$.workspace.environments.default.recipe.location");

        assertThat(dockerImage).isEqualToIgnoringWhitespace("rhche/spring-boot");

        String projectDescription = JsonPath.read(factoryJson, "$.workspace.projects[0].description");

        assertThat(projectDescription).isEqualToIgnoringWhitespace("My Description");

        String projectName = JsonPath.read(factoryJson, "$.workspace.projects[0].name");

        assertThat(projectName).isEqualToIgnoringWhitespace("demo-project");

        String projectPath = JsonPath.read(factoryJson, "$.workspace.projects[0].path");

        assertThat(projectPath).isEqualToIgnoringWhitespace("/demo-project");

        String workspaceName = JsonPath.read(factoryJson, "$.workspace.name");

        assertThat(workspaceName).isEqualToIgnoringWhitespace("demo-project");
    }

}
