package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSecurityGroupsResponse implements JsonEntity {

    private Map<String, Set<PlatformSecurityGroupResponse>> securityGroups = new HashMap<>();

    public PlatformSecurityGroupsResponse(Map<String, Set<PlatformSecurityGroupResponse>> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public Map<String, Set<PlatformSecurityGroupResponse>> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(Map<String, Set<PlatformSecurityGroupResponse>> securityGroups) {
        this.securityGroups = securityGroups;
    }
}
