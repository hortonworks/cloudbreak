package com.sequenceiq.cloudbreak.service.filesystem.resource.definition;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudFileSystemSupportConfigEntries {

    private String provider;

    private Set<CloudFileSystemSupportConfigEntry> configEntries = new HashSet<>();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Set<CloudFileSystemSupportConfigEntry> getConfigEntries() {
        return configEntries;
    }

    public void setConfigEntries(Set<CloudFileSystemSupportConfigEntry> configEntries) {
        this.configEntries = configEntries;
    }
}
