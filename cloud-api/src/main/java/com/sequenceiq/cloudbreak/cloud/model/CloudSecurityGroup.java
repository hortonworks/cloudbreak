package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

public class CloudSecurityGroup {

    private String groupName;

    private String groupId;

    private Map<String, Object> properties = new HashMap<>();

    public CloudSecurityGroup(String groupName, String groupId, Map<String, Object> properties) {
        this.groupName = groupName;
        this.groupId = groupId;
        this.properties = properties;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
