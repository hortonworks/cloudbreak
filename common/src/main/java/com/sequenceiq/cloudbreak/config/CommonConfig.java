package com.sequenceiq.cloudbreak.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.sequenceiq.cloudbreak.spring.YamlPropertySourceFactory;

/**
 * This configuration reads the common-config.yml partial Spring config from the classpath
 * Then it tries to override with the content of the same named file under cb.etc.config.dir
 *
 * The @PropertySource annotation order is important!
 */
@Configuration
@PropertySource(value = "classpath:common-config.yml", factory = YamlPropertySourceFactory.class)
@PropertySource(value = "file:${cb.etc.config.dir:}/common-config.yml", ignoreResourceNotFound = true, factory = YamlPropertySourceFactory.class)
public class CommonConfig {
}
