package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSecurityGroupsV4Response implements JsonEntity {

    private Map<String, Set<PlatformSecurityGroupV4Response>> securityGroups = new HashMap<>();

    public PlatformSecurityGroupsV4Response() {
    }

    public PlatformSecurityGroupsV4Response(Map<String, Set<PlatformSecurityGroupV4Response>> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public Map<String, Set<PlatformSecurityGroupV4Response>> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(Map<String, Set<PlatformSecurityGroupV4Response>> securityGroups) {
        this.securityGroups = securityGroups;
    }
}
