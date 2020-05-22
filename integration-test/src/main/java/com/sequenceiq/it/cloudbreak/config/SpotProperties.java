package com.sequenceiq.it.cloudbreak.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.spot")
public class SpotProperties {

    private List<String> enabledCloudPlatforms;

    public List<String> getEnabledCloudPlatforms() {
        return enabledCloudPlatforms;
    }

    public void setEnabledCloudPlatforms(List<String> enabledCloudPlatforms) {
        this.enabledCloudPlatforms = enabledCloudPlatforms;
    }
}
