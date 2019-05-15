package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformIpPoolsV1Response implements Serializable {

    private Map<String, Set<IpPoolV1Response>> ippools = new HashMap<>();

    public PlatformIpPoolsV1Response() {
    }

    public PlatformIpPoolsV1Response(Map<String, Set<IpPoolV1Response>> ippools) {
        this.ippools = ippools;
    }

    public Map<String, Set<IpPoolV1Response>> getIppools() {
        return ippools;
    }

    public void setIppools(Map<String, Set<IpPoolV1Response>> ippools) {
        this.ippools = ippools;
    }
}
