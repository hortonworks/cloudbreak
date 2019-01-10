package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.AccessConfigJson;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformAccessConfigsV4Response implements JsonEntity {

    private Set<AccessConfigJson> accessConfigs = new HashSet<>();

    public PlatformAccessConfigsV4Response() {
    }

    public Set<AccessConfigJson> getAccessConfigs() {
        return accessConfigs;
    }

    public void setAccessConfigs(Set<AccessConfigJson> accessConfigs) {
        this.accessConfigs = accessConfigs;
    }
}
