package com.redhat.developers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public freemarker.template.Configuration freemarkerConfig() {

        freemarker.template.Configuration ftlConfig =
                new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_20);


        return ftlConfig;
    }
}
