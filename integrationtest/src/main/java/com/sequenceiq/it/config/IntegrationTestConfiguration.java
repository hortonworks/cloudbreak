package com.sequenceiq.it.config;

import java.io.IOException;

import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.testng.TestNG;

import com.sequenceiq.it.SuiteContext;

import freemarker.template.TemplateException;

@Configuration
@ComponentScan("com.sequenceiq.it")
@ConfigurationProperties
public class IntegrationTestConfiguration {
    @Bean
    public static PropertyResourceConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public SuiteContext suiteContext() {
        return new SuiteContext();
    }

    @Bean
    public freemarker.template.Configuration freemarkerConfiguration() throws IOException, TemplateException {
        FreeMarkerConfigurationFactory factory = new FreeMarkerConfigurationFactory();
        factory.setPreferFileSystemAccess(false);
        factory.setTemplateLoaderPath("classpath:/");
        return factory.createConfiguration();
    }

    @Bean
    public TestNG testNG() {
        return new TestNG();
    }
}
