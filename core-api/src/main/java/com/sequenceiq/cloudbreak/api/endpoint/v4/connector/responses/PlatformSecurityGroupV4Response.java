package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSecurityGroupV4Response implements JsonEntity {

    private String groupName;

    private String groupId;

    private Map<String, Object> properties;

    public PlatformSecurityGroupV4Response(String groupName, String groupId, Map<String, Object> properties) {
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
