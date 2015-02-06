package com.sequenceiq.it.cloudbreak.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.it.cloudbreak.TemplateAdditionParser;

@Configuration
public class CloudbreakITConfig {
    @Bean
    TemplateAdditionParser templateAdditionParser() {
        return new TemplateAdditionParser();
    }
}
