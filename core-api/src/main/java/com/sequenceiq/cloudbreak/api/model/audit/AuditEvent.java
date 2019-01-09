package com.sequenceiq.cloudbreak.api.model.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.annotations.Immutable;
import com.sequenceiq.cloudbreak.api.model.annotations.TransformGetterType;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditEvent {

    private Long auditId;

    @TransformGetterType
    private StructuredEvent structuredEvent;

    public AuditEvent() {
    }

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

    public StructuredEvent getStructuredEvent() {
        return structuredEvent;
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
