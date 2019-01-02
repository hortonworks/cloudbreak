package com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class CloudbreakEventBaseV4 implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.EventModelDescription.TYPE)
    private String eventType;

    @ApiModelProperty(ModelDescriptions.EventModelDescription.TIMESTAMP)
    private long eventTimestamp;

    @ApiModelProperty(ModelDescriptions.EventModelDescription.MESSAGE)
    private String eventMessage;

    @ApiModelProperty(ModelDescriptions.USER_ID)
    private String userId;

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
}
