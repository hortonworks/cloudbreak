package com.sequenceiq.cloudbreak.api.model.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AuditEvent {

    private final Long auditId;

    private final StructuredEvent structuredEvent;

    public AuditEvent(Long auditId, StructuredEvent structuredEvent) {
        this.auditId = auditId;
        this.structuredEvent = structuredEvent;
    }

    public OperationDetails getOperation() {
        return structuredEvent.getOperation();
    }

    public Long getAuditId() {
        return auditId;
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
