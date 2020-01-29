package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformIpPoolsResponse implements Serializable {

    private Map<String, Set<IpPoolResponse>> ippools = new HashMap<>();

    public PlatformIpPoolsResponse() {
    }

    public PlatformIpPoolsResponse(Map<String, Set<IpPoolResponse>> ippools) {
        this.ippools = ippools;
    }

    public Map<String, Set<IpPoolResponse>> getIppools() {
        return ippools;
    }

    public void setIppools(Map<String, Set<IpPoolResponse>> ippools) {
        this.ippools = ippools;
    }

    @Override
    public String toString() {
        return "PlatformIpPoolsResponse{" +
                "ippools=" + ippools +
                '}';
    }
}
