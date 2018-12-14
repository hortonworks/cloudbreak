package com.sequenceiq.cloudbreak.api.model.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredEventResponse {

    public static final String TYPE_FIELD = "type";

    @ApiModelProperty(ModelDescriptions.StructuredEventResponseDescription.TYPE)
    private String type;

    @ApiModelProperty(ModelDescriptions.StructuredEventResponseDescription.OPERATION)
    private OperationDetails operation;

    @ApiModelProperty(ModelDescriptions.StructuredEventResponseDescription.EVENT_JSON)
    private String eventJson;

    @ApiModelProperty(ModelDescriptions.StructuredEventResponseDescription.STATUS)
    private String status;

    @ApiModelProperty(ModelDescriptions.StructuredEventResponseDescription.DURATION)
    private Long duration;

    public static String getTypeField() {
        return TYPE_FIELD;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public OperationDetails getOperation() {
        return operation;
    }

    public void setOperation(OperationDetails operation) {
        this.operation = operation;
    }

    @JsonIgnore
    public String getEventJson() {
        return eventJson;
    }

    public void setEventJson(String eventJson) {
        this.eventJson = eventJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    @JsonRawValue
    public String getRawFlowEvent() {
        return getType().equals(StructuredFlowEvent.class.getSimpleName()) ? getEventJson() : null;
    }

    @JsonRawValue
    public String getRawRestEvent() {
        return getType().equals(StructuredRestCallEvent.class.getSimpleName()) ? getEventJson() : null;
    }

    @JsonRawValue
    public String getRawNotification() {
        return getType().equals(StructuredNotificationEvent.class.getSimpleName()) ? getEventJson() : null;
    }
}
