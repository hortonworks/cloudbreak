package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.Calendar;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationDetails {
    private String eventType;

    private Long resourceId;

    private String resourceType;

    private Date timestamp;

    private String account;

    private String userId;

    private String cloudbreakId;

    private String cloudbreakVersion;

    private OperationDetails() {
    }

    public OperationDetails(String eventType, String resourceType, Long resourceId, String account, String userId, String cloudbreakId,
            String cloudbreakVersion) {
        this.timestamp = Calendar.getInstance().getTime();
        this.eventType = eventType;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.account = account;
        this.userId = userId;
        this.cloudbreakId = cloudbreakId;
        this.cloudbreakVersion = cloudbreakVersion;
    }

    public String getEventType() {
        return eventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public Date getTimestamp() {
        return timestamp;
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
}
