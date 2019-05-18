package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformAccessConfigsV1Response implements Serializable {

    private Set<AccessConfigV1Response> accessConfigs = new HashSet<>();

    public Set<AccessConfigV1Response> getAccessConfigs() {
        return accessConfigs;
    }

    public void setAccessConfigs(Set<AccessConfigV1Response> accessConfigs) {
        this.accessConfigs = accessConfigs;
    }
}
