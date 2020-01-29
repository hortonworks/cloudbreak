package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSecurityGroupsResponse implements Serializable {

    private Map<String, Set<PlatformSecurityGroupResponse>> securityGroups = new HashMap<>();

    public PlatformSecurityGroupsResponse() {
    }

    public PlatformSecurityGroupsResponse(Map<String, Set<PlatformSecurityGroupResponse>> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public Map<String, Set<PlatformSecurityGroupResponse>> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(Map<String, Set<PlatformSecurityGroupResponse>> securityGroups) {
        this.securityGroups = securityGroups;
    }

    @Override
    public String toString() {
        return "PlatformSecurityGroupsResponse{" +
                "securityGroups=" + securityGroups +
                '}';
    }
}
