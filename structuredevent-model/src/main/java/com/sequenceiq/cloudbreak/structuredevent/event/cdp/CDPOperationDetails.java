package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPOperationDetails implements Serializable {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss Z");

    private StructuredEventType eventType;

    private Long resourceId;

    private String resourceCrn;

    private String resourceName;

    private String resourceType;

    private Long timestamp;

    private String userId;

    private String userName;

    private String cloudbreakId;

    private String cloudbreakVersion;

    private String tenant;

    private String uuid;

    private String userCrn;

    private String environmentCrn;

    private String resourceEvent;

    public CDPOperationDetails() {
    }

    public CDPOperationDetails(Long timestamp, StructuredEventType eventType, String resourceType, Long resourceId, String resourceName, String cloudbreakId,
        String cloudbreakVersion, String userId, String userName, String tenant, String resourceCrn, String userCrn,
        String environmentCrn, String resourceEvent) {
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.cloudbreakId = cloudbreakId;
        this.cloudbreakVersion = cloudbreakVersion;
        this.userId = userId;
        this.userName = userName;
        this.tenant = tenant;
        this.resourceCrn = resourceCrn;
        this.userCrn = userCrn;
        this.environmentCrn = environmentCrn;
        this.resourceEvent = resourceEvent;
        uuid = UUID.randomUUID().toString();
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

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUTCDateTime() {
        if (timestamp != null) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC).format(DATE_TIME_FORMATTER);
        } else {
            return "";
        }
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

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public void setUserCrn(String userCrn) {
        this.userCrn = userCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getResourceEvent() {
        return resourceEvent;
    }

    public void setResourceEvent(String resourceEvent) {
        this.resourceEvent = resourceEvent;
    }



}
