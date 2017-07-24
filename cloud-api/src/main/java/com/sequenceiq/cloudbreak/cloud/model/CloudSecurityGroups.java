package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CloudSecurityGroups {

    private Map<String, Set<CloudSecurityGroup>> cloudSecurityGroupsResponses = new HashMap<>();

    public CloudSecurityGroups() {
    }

    public CloudSecurityGroups(Map<String, Set<CloudSecurityGroup>> cloudSecurityGroupsResponses) {
        this.cloudSecurityGroupsResponses = cloudSecurityGroupsResponses;
    }

    public Map<String, Set<CloudSecurityGroup>> getCloudSecurityGroupsResponses() {
        return cloudSecurityGroupsResponses;
    }

    public void setCloudSecurityGroupsResponses(Map<String, Set<CloudSecurityGroup>> cloudSecurityGroupsResponses) {
        this.cloudSecurityGroupsResponses = cloudSecurityGroupsResponses;
    }
}
