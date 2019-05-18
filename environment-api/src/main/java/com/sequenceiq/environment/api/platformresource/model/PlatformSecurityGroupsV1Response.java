package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSecurityGroupsV1Response implements Serializable {

    private Map<String, Set<PlatformSecurityGroupV1Response>> securityGroups = new HashMap<>();

    public PlatformSecurityGroupsV1Response() {
    }

    public PlatformSecurityGroupsV1Response(Map<String, Set<PlatformSecurityGroupV1Response>> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public Map<String, Set<PlatformSecurityGroupV1Response>> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(Map<String, Set<PlatformSecurityGroupV1Response>> securityGroups) {
        this.securityGroups = securityGroups;
    }
}
