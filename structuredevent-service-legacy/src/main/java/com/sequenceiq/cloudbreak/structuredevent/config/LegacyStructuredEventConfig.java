package com.sequenceiq.cloudbreak.structuredevent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.structuredevent.LegacyDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.LegacyStructuredEventClient;

@Configuration
public class LegacyStructuredEventConfig {
    @Bean
    public LegacyStructuredEventClient structuredEventClient() {
        return new LegacyDefaultStructuredEventClient();
    }
}
