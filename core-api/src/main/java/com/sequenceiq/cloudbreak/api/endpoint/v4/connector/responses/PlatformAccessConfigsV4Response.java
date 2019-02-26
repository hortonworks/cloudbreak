package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformAccessConfigsV4Response implements JsonEntity {

    private Set<AccessConfigV4Response> accessConfigs = new HashSet<>();

    public Set<AccessConfigV4Response> getAccessConfigs() {
        return accessConfigs;
    }

    public void setAccessConfigs(Set<AccessConfigV4Response> accessConfigs) {
        this.accessConfigs = accessConfigs;
    }
}
