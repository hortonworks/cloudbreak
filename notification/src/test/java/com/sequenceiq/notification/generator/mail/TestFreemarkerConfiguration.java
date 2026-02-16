package com.sequenceiq.notification.generator.mail;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import freemarker.template.Configuration;
import freemarker.template.Version;

@TestConfiguration
public class TestFreemarkerConfiguration {

    @Bean
    public Configuration freemarkerConfiguration() {
        Configuration config = new Configuration(new Version("2.3.31"));
        config.setClassForTemplateLoading(getClass(), "/");
        return config;
    }
}
