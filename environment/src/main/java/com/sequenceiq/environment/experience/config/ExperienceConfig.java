package com.sequenceiq.environment.experience.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.sequenceiq.cloudbreak.spring.YamlPropertySourceFactory;

/**
 * This configuration reads the experiences-config.yaml partial Spring config from the classpath
 * Then it tries to override with the content of the same named file under cb.etc.config.dir
 *
 * The @PropertySource annotation order is important!
 */
@Configuration
@PropertySource(value = "classpath:experiences-config.yml", factory = YamlPropertySourceFactory.class)
@PropertySource(value = "file:${cb.etc.config.dir:}/experiences-config.yml", ignoreResourceNotFound = true, factory = YamlPropertySourceFactory.class)
public class ExperienceConfig {
}
