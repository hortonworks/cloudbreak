package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformIpPoolsResponse implements JsonEntity {

    private Map<String, Set<IpPoolJson>> ippools = new HashMap<>();

    public PlatformIpPoolsResponse() {
    }

    public PlatformIpPoolsResponse(Map<String, Set<IpPoolJson>> ippools) {
        this.ippools = ippools;
    }

    public Map<String, Set<IpPoolJson>> getIppools() {
        return ippools;
    }

    public void setIppools(Map<String, Set<IpPoolJson>> ippools) {
        this.ippools = ippools;
    }
}
