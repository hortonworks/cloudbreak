package com.sequenceiq.cloudbreak.service.filesystem.resource.definition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudFileSystemSupportConfigEntry {

    private String supportedFileSytem;

    private String minVersion;

    public String getSupportedFileSytem() {
        return supportedFileSytem;
    }

    public void setSupportedFileSytem(String supportedFileSytem) {
        this.supportedFileSytem = supportedFileSytem;
    }

    public String getMinVersion() {
        return minVersion;
    }

    public void setMinVersion(String minVersion) {
        this.minVersion = minVersion;
    }
}
