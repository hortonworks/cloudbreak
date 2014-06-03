package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Configuration
public class PropertyConfiguration {

    @Bean
    public PropertyPlaceholderConfigurer propertyConfigurer() throws IOException {
        PropertyPlaceholderConfigurer props = new PropertyPlaceholderConfigurer();
        props.setLocations(new Resource[] { new ClassPathResource("default.properties") });
        props.setSystemPropertiesModeName("SYSTEM_PROPERTIES_MODE_OVERRIDE");
        return props;
    }

}
