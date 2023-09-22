package com.sequenceiq.periscope.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImpalaConfig {

    @Value("${periscope.impala.minimum.executor.nodes:1}")
    private Integer minimumNodes;

    public Integer getMinimumNodes() {
        return minimumNodes;
    }
}
