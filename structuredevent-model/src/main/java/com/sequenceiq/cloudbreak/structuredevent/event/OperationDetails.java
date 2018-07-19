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

    private String resourceType;

    private Long timestamp;

    private ZonedDateTime zonedDateTime;

    private String account;

    private String userId;

    private String userName;

    private String cloudbreakId;

    private String cloudbreakVersion;

    public OperationDetails() {
    }

    public OperationDetails(StructuredEventType eventType, String resourceType, Long resourceId, String account, String userId, String userName, String cloudbreakId,
            String cloudbreakVersion) {
        this(Calendar.getInstance().getTimeInMillis(), eventType, resourceType, resourceId, account, userId, userName, cloudbreakId, cloudbreakVersion);
    }

    public OperationDetails(Long timestamp, StructuredEventType eventType, String resourceType, Long resourceId, String account, String userId, String userName,
            String cloudbreakId, String cloudbreakVersion) {
        this.timestamp = timestamp;
        zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC);
        this.eventType = eventType;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.account = account;
        this.userId = userId;
        this.userName = userName;
        this.cloudbreakId = cloudbreakId;
        this.cloudbreakVersion = cloudbreakVersion;
    }

    public void setEventType(StructuredEventType eventType) {
        this.eventType = eventType;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setCloudbreakId(String cloudbreakId) {
        this.cloudbreakId = cloudbreakId;
    }

    public void setCloudbreakVersion(String cloudbreakVersion) {
        this.cloudbreakVersion = cloudbreakVersion;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public StructuredEventType getEventType() {
        return eventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public String getAccount() {
        return account;
    }

    public String getUserId() {
        return userId;
    }

    public String getCloudbreakId() {
        return cloudbreakId;
    }

    public String getCloudbreakVersion() {
        return cloudbreakVersion;
    }

    public String getUserName() {
        return userName;
    }
}
