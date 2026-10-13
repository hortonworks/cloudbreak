package com.sequenceiq.cloudbreak.conf;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.sequenceiq.cloudbreak.javaversion.JavaVersionPolicy;
import com.sequenceiq.cloudbreak.spring.YamlPropertySourceFactory;

/**
 * This configuration reads the service-common-config.yml partial Spring config from the classpath
 * Then it tries to override with the content of the same named file under cb.etc.config.dir
 *
 * The @PropertySource annotation order is important!
 *
 * <p>Runtime-version policy lives in its own file rather than in common-config.yml so that a deployment can
 * override the whole policy - and nothing else - by mounting a single file under {@code cb.etc.config.dir}.</p>
 */
@Configuration
@PropertySource(value = "classpath:service-common-config.yml", factory = YamlPropertySourceFactory.class)
@PropertySource(value = "file:${cb.etc.config.dir:}/service-common-config.yml", ignoreResourceNotFound = true, factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties(JavaVersionPolicy.class)
public class ServiceCommonConfig {
}
