package com.sequenceiq.freeipa.configuration;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;

@Configuration
@ConfigurationProperties(prefix = "freeipa.scaling")
public class AllowedScalingPaths {

    private Map<AvailabilityType, List<AvailabilityType>> paths;

    public Map<AvailabilityType, List<AvailabilityType>> getPaths() {
        return paths;
    }

    public void setPaths(Map<AvailabilityType, List<AvailabilityType>> paths) {
        this.paths = paths;
    }
}
