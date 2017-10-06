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
package com.redhat.developers;

import com.jayway.jsonpath.JsonPath;
import com.redhat.developers.service.TemplateService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableAutoConfiguration
@ActiveProfiles("ut")
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
