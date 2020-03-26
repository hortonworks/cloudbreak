package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformIpPoolsV4Response implements JsonEntity {

    private Map<String, Set<IpPoolV4Response>> ippools = new HashMap<>();

    public PlatformIpPoolsV4Response() {
    }

    public PlatformIpPoolsV4Response(Map<String, Set<IpPoolV4Response>> ippools) {
        this.ippools = ippools;
    }

    public Map<String, Set<IpPoolV4Response>> getIppools() {
        return ippools;
    }

    public void setIppools(Map<String, Set<IpPoolV4Response>> ippools) {
        this.ippools = ippools;
    }
}
