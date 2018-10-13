package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationDetails implements Serializable {

    private StructuredEventType eventType;

    private Long resourceId;

    private String resourceName;

    private String resourceType;

    private Long timestamp;

    private String userIdV3;

    private String userNameV3;

    private String userId;

    private String userName;

    private String cloudbreakId;

    private String cloudbreakVersion;

    private Long workspaceId;

    public OperationDetails() {
    }

    public OperationDetails(StructuredEventType eventType, String resourceType, Long resourceId, String resourceName, String userIdV3,
            String userNameV3, String cloudbreakId, String cloudbreakVersion, Long workspaceId, String userId, String userName) {
        this(Calendar.getInstance().getTimeInMillis(), eventType, resourceType, resourceId, resourceName, userIdV3,
                userNameV3, cloudbreakId, cloudbreakVersion, workspaceId, userId, userName);
    }

    public OperationDetails(Long timestamp, StructuredEventType eventType, String resourceType, Long resourceId,
            String resourceName, String userIdV3, String userNameV3, String cloudbreakId,
            String cloudbreakVersion, Long workspaceId, String userId, String userName) {
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.userIdV3 = userIdV3;
        this.userNameV3 = userNameV3;
        this.cloudbreakId = cloudbreakId;
        this.cloudbreakVersion = cloudbreakVersion;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.userName = userName;
    }

    public StructuredEventType getEventType() {
        return eventType;
    }

    public void setEventType(StructuredEventType eventType) {
        this.eventType = eventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public ZonedDateTime getZonedDateTime() {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
    }

    public String getUserIdV3() {
        return userIdV3;
    }

    public void setUserIdV3(String userIdV3) {
        this.userIdV3 = userIdV3;
    }

    public String getCloudbreakId() {
        return cloudbreakId;
    }

    public void setCloudbreakId(String cloudbreakId) {
        this.cloudbreakId = cloudbreakId;
    }

    public String getCloudbreakVersion() {
        return cloudbreakVersion;
    }

    public void setCloudbreakVersion(String cloudbreakVersion) {
        this.cloudbreakVersion = cloudbreakVersion;
    }

    public String getUserNameV3() {
        return userNameV3;
    }

    public void setUserNameV3(String userNameV3) {
        this.userNameV3 = userNameV3;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
