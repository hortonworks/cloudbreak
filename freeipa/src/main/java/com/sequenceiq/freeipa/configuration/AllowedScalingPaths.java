package com.sequenceiq.freeipa.configuration;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.FormFactor;

@Configuration
@ConfigurationProperties(prefix = "freeipa.scaling")
public class AllowedScalingPaths {

    private Map<FormFactor, List<FormFactor>> paths;

    public Map<FormFactor, List<FormFactor>> getPaths() {
        return paths;
    }

    public void setPaths(Map<FormFactor, List<FormFactor>> paths) {
        this.paths = paths;
    }
}
