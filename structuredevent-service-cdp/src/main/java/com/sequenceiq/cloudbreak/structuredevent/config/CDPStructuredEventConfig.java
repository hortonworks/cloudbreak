package com.sequenceiq.cloudbreak.structuredevent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.CDPDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredEventClient;

@Configuration
public class CDPStructuredEventConfig {
    @Bean
    public CDPStructuredEventClient structuredEventClient() {
        return new CDPDefaultStructuredEventClient();
    }
}
