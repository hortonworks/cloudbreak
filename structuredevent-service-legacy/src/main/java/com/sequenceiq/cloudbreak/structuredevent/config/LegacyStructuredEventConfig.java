package com.sequenceiq.cloudbreak.structuredevent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.structuredevent.LegacyBaseStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.LegacyDefaultStructuredEventClient;

@Configuration
public class LegacyStructuredEventConfig {
    @Bean
    public LegacyBaseStructuredEventClient structuredEventClient() {
        return new LegacyDefaultStructuredEventClient();
    }
}
