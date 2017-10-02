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
package com.redhat.developers.config;

import com.redhat.developers.metadata.StaticInitializrMetadataProvider;
import com.redhat.developers.metadata.VersionedDependencyMetadataProvider;
import com.redhat.developers.project.CheProjectController;
import com.redhat.developers.service.GitHubRepoService;
import com.redhat.developers.service.TemplateService;
import io.spring.initializr.generator.ProjectGenerator;
import io.spring.initializr.generator.ProjectRequestPostProcessor;
import io.spring.initializr.generator.ProjectRequestResolver;
import io.spring.initializr.generator.ProjectResourceLocator;
import io.spring.initializr.metadata.*;
import io.spring.initializr.util.TemplateRenderer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(value = {InitializrProperties.class, CheBootalizrProperties.class})
public class CheBootializrConfiguration {

    private final List<ProjectRequestPostProcessor> postProcessors;

    public CheBootializrConfiguration(ObjectProvider<List<ProjectRequestPostProcessor>> postProcessors) {
        List<ProjectRequestPostProcessor> listOfPostProcessors = postProcessors.getIfAvailable();
        this.postProcessors = listOfPostProcessors != null ? listOfPostProcessors : new ArrayList<>();
    }

    @Bean
    @ConditionalOnMissingBean(GitHubRepoService.class)
    public GitHubRepoService repoService(CheBootalizrProperties cheBootalizrProperties) {
        return new GitHubRepoService(cheBootalizrProperties);
    }

    @Bean
    public TemplateRenderer templateRenderer(Environment environment) {
        RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment,
            "spring.mustache.");
        boolean cache = resolver.getProperty("cache", Boolean.class, true);
        TemplateRenderer templateRenderer = new TemplateRenderer();
        templateRenderer.setCache(cache);
        return templateRenderer;
    }

    @Bean
    public ProjectGenerator projectGenerator() {
        return new ProjectGenerator();
    }

    @Bean
    public ProjectRequestResolver projectRequestResolver() {
        return new ProjectRequestResolver(this.postProcessors);
    }

    @Bean
    public ProjectResourceLocator projectResourceLocator() {
        return new ProjectResourceLocator();
    }

    @Bean
    public DependencyMetadataProvider dependencyMetadataProvider() {
        return new VersionedDependencyMetadataProvider();
    }

    @Bean
    public InitializrMetadataProvider initializrMetadataProvider(InitializrProperties properties) {
        InitializrMetadata metadata = InitializrMetadataBuilder
            .fromInitializrProperties(properties)
            .build();

        return new StaticInitializrMetadataProvider(metadata);
    }

}
