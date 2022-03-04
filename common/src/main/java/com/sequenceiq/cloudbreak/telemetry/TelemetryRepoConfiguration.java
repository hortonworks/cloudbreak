package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("telemetry.repo")
public class TelemetryRepoConfiguration {

    private String name;

    private String baseUrl;

    private String gpgKey;

    private Integer gpgCheck;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getGpgKey() {
        return gpgKey;
    }

    public void setGpgKey(String gpgKey) {
        this.gpgKey = gpgKey;
    }

    public Integer getGpgCheck() {
        return gpgCheck;
    }

    public void setGpgCheck(Integer gpgCheck) {
        this.gpgCheck = gpgCheck;
    }
}
