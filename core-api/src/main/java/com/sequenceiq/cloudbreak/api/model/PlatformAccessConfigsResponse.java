package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformAccessConfigsResponse implements JsonEntity {

    private Set<AccessConfigJson> accessConfigs = new HashSet<>();

    public Set<AccessConfigJson> getAccessConfigs() {
        return accessConfigs;
    }

    public void setAccessConfigs(Set<AccessConfigJson> accessConfigs) {
        this.accessConfigs = accessConfigs;
    }
}
