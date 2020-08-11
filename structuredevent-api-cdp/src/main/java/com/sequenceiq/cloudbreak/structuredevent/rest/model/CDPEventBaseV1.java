package com.sequenceiq.cloudbreak.structuredevent.rest.model;

import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CDPEventBaseV1 implements JsonEntity {

    private String eventType;

    private long eventTimestamp;

    private String eventMessage;

    private String userId;

    private String notificationType;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(long eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getEventMessage() {
        return eventMessage;
    }

    public void setEventMessage(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }
}
