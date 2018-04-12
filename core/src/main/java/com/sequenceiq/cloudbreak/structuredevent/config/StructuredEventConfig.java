package com.sequenceiq.cloudbreak.structuredevent.config;

import com.sequenceiq.cloudbreak.structuredevent.DefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StructuredEventConfig {
    @Bean
    public StructuredEventClient structuredEventClient() {
        return new DefaultStructuredEventClient();
    }
}
