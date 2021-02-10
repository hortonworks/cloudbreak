package com.sequenceiq.cloudbreak.spring;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

/**
 * This class can be used as a custom {@link PropertySourceFactory} on {@link Configuration} classes
 * to read YAML files as configuration properties
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlPropertySourceFactory.class);

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource encodedResource) throws IOException {
        LOGGER.debug("Trying to load configuration resource {}", encodedResource.getResource().getURI());
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(encodedResource.getResource());
        try {
            Properties properties = factory.getObject();
            return new PropertiesPropertySource(encodedResource.getResource().getFilename(), properties);
        } catch (Exception e) {
            LOGGER.debug("Configuration resource ({}) cannot be loaded", encodedResource.getResource().getURI());
            // WARNING: Do Not Change the type of the exception below as it is checked by Spring
            throw new FileNotFoundException(String.format("Configuration resource (%s) cannot be loaded", encodedResource.getResource().getURI()));
        }
    }
}
