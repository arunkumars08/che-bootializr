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

import com.redhat.developers.config.CheBootalizrProperties;
import com.redhat.developers.config.CheBootializrConfiguration;
import io.spring.initializr.metadata.InitializrMetadataProvider;
import io.spring.initializr.metadata.InitializrProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableAutoConfiguration
@ActiveProfiles("ut")
public class CheBootializrApplicationTests {

    @Autowired
    private CheBootalizrProperties cheBootalizrProperties;

    @Autowired
    private InitializrMetadataProvider initializrMetadataProvider;

    @Test
    public void test_properties_are_loaded() {
        assertThat(cheBootalizrProperties).isNotNull();
        assertThat(cheBootalizrProperties.getChe()).isNotNull();
        assertThat(cheBootalizrProperties.getChe().getServiceUrl()).isNotNull();
        assertThat(cheBootalizrProperties.getChe().getServiceUrl()).isEqualTo("http://che.example.com");
    }


    @Test
    public void test_booster_properties_are_loaded() {
        assertThat(cheBootalizrProperties).isNotNull();
        assertThat(cheBootalizrProperties.getBoosterCatalog()).isNotNull();
        assertThat(cheBootalizrProperties.getBoosterCatalog().getCatalogRef()).isNotNull();
        assertThat(cheBootalizrProperties.getBoosterCatalog().getCatalogRef()).isEqualTo("master");
        assertThat(cheBootalizrProperties.getBoosterCatalog().getCatalogRepository()).isEqualTo("https://github.com/kameshsampath/booster-catalog.git");
    }

    @Test
    public void should_load_static_boot_versions() {
        assertThat(initializrMetadataProvider).isNotNull();
        assertThat(initializrMetadataProvider.get().getBootVersions().getContent()).isNotNull();
        assertThat(initializrMetadataProvider.get().getBootVersions().getContent().size()).isEqualTo(2);
        assertThat(initializrMetadataProvider.get().getBootVersions().getContent().get(0).getName()).isEqualTo("1.5.7");
    }

    @Test
    public void should_have_maven_project_type() {
        assertThat(initializrMetadataProvider).isNotNull();
        assertThat(initializrMetadataProvider.get().getTypes().getContent()).isNotNull();
        assertThat(initializrMetadataProvider.get().getTypes().getContent().size()).isEqualTo(1);
        assertThat(initializrMetadataProvider.get().getTypes().getContent().get(0).getName()).isEqualTo("Maven Project");
    }

}
