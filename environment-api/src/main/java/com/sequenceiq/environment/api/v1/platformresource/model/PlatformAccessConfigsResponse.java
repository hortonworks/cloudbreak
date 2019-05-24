package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformAccessConfigsResponse implements Serializable {

    private Set<AccessConfigResponse> accessConfigs = new HashSet<>();

    public Set<AccessConfigResponse> getAccessConfigs() {
        return accessConfigs;
    }

    public void setAccessConfigs(Set<AccessConfigResponse> accessConfigs) {
        this.accessConfigs = accessConfigs;
    }
}
