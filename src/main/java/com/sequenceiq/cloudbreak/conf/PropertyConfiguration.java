package com.sequenceiq.cloudbreak.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class PropertyConfiguration {

    @Bean
    public PropertySourcesPlaceholderConfigurer propertyConfigurer() throws IOException {
        PropertySourcesPlaceholderConfigurer props = new PropertySourcesPlaceholderConfigurer();
        props.setLocations(new Resource[]{new ClassPathResource("default.properties")});
        return props;
    }
}
