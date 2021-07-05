package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSecurityGroupResponse implements Serializable {

    private String groupName;

    private String groupId;

    private Map<String, Object> properties;

    public PlatformSecurityGroupResponse(String groupName, String groupId, Map<String, Object> properties) {
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

    @Override
    public String toString() {
        return "PlatformSecurityGroupResponse{" +
                "groupName='" + groupName + '\'' +
                ", groupId='" + groupId + '\'' +
                ", properties=" + properties +
                '}';
    }
}
