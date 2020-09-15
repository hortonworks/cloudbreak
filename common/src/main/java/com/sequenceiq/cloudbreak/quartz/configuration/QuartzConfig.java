package com.sequenceiq.cloudbreak.quartz.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:quartz.properties")
public class QuartzConfig {
}
