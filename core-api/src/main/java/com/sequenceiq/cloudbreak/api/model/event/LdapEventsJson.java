package com.sequenceiq.cloudbreak.api.model.event;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EventModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("LdapEvent")
public class LdapEventsJson implements JsonEntity {

    @ApiModelProperty(EventModelDescription.TYPE)
    private String eventType;

    @ApiModelProperty(EventModelDescription.TIMESTAMP)
    private long eventTimestamp;

    @ApiModelProperty(EventModelDescription.MESSAGE)
    private String eventMessage;

    @ApiModelProperty(ModelDescriptions.USER_ID)
    private String userId;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_ID)
    private Long workspaceId;

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

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}
