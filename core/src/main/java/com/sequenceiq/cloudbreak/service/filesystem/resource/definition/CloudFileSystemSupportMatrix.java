package com.sequenceiq.cloudbreak.service.filesystem.resource.definition;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudFileSystemSupportMatrix {

    private Set<CloudFileSystemSupportConfigEntries> providers = new HashSet<>();

    public Set<CloudFileSystemSupportConfigEntries> getProviders() {
        return providers;
    }

    public void setProviders(Set<CloudFileSystemSupportConfigEntries> providers) {
        this.providers = providers;
    }
}
