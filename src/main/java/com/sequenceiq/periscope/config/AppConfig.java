package com.sequenceiq.periscope.config;

import java.net.MalformedURLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.Yaml;

@Configuration
@EnableScheduling
public class AppConfig {

    private static final String PERISCOPE_YAML = "periscope.yaml";

    @Value("${periscope.config:}")
    private String configLocation;

    @Bean
    ThreadPoolExecutorFactoryBean getThreadPoolExecutorFactoryBean() {
        return new ThreadPoolExecutorFactoryBean();
    }

    @Bean
    RestOperations createRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    Yaml createYml() {
        return new Yaml();
    }

    @Bean
    Resource cloudbreakSettings() throws MalformedURLException {
        if (configLocation.isEmpty()) {
            return new ClassPathResource(PERISCOPE_YAML);
        } else {
            return new UrlResource(configLocation);
        }
    }

}