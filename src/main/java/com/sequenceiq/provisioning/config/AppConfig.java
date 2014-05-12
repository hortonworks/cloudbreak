package com.sequenceiq.provisioning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;

@Configuration
public class AppConfig {

    @Bean
    public AmazonCloudFormationClient amazonCloudFormationClient() {
        return new AmazonCloudFormationClient();
    }

}
