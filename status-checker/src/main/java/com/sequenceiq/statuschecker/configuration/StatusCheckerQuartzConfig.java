package com.sequenceiq.statuschecker.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:statuschecker.properties")
public class StatusCheckerQuartzConfig {
}
