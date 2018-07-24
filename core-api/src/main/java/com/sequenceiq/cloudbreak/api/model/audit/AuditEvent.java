package com.sequenceiq.cloudbreak.api.model.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent {

    private StructuredEvent structuredEvent;

    public AuditEvent(StructuredEvent structuredEvent) {
        this.structuredEvent = structuredEvent;
    }

    public OperationDetails getOperation() {
        return structuredEvent.getOperation();
    }

    public String getStatus() {
        return structuredEvent.getStatus();
    }

    public Long getDuration() {
        return structuredEvent.getDuration();
    }

    public StructuredFlowEvent getRawFlowEvent() {
        return structuredEvent instanceof StructuredFlowEvent ? (StructuredFlowEvent) structuredEvent : null;
    }

    public StructuredRestCallEvent getRawRestEvent() {
        return structuredEvent instanceof StructuredRestCallEvent ? (StructuredRestCallEvent) structuredEvent : null;
    }

    public StructuredNotificationEvent getRawNotification() {
        return structuredEvent instanceof StructuredNotificationEvent ? (StructuredNotificationEvent) structuredEvent : null;
    }

}
