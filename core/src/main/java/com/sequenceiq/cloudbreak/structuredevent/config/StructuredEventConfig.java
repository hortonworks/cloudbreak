package com.sequenceiq.cloudbreak.structuredevent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.structuredevent.StructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.DefaultStructuredEventClient;

@Configuration
public class StructuredEventConfig {
    @Bean
    public StructuredEventClient structuredEventClient() {
        return new DefaultStructuredEventClient();
    }
}
