package com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class CloudbreakV4Event {

    @ApiModelProperty(ModelDescriptions.EventModelDescription.TYPE)
    private NotificationEventType eventType;

    @ApiModelProperty
    private Object payload;

    @ApiModelProperty
    private String resourceName;

    @ApiModelProperty
    private String user;

    @ApiModelProperty
    private Long timestamp;

    @ApiModelProperty
    private Long workspaceId;

    @ApiModelProperty
    private String message;

    public NotificationEventType getEventType() {
        return eventType;
    }

    public void setEventType(NotificationEventType eventType) {
        this.eventType = eventType;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
