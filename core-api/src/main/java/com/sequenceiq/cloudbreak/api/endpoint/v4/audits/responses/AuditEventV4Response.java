package com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.annotations.Immutable;
import com.sequenceiq.common.model.annotations.TransformGetterType;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditEventV4Response {

    private Long auditId;

    @TransformGetterType
    private StructuredEvent structuredEvent;

    public AuditEventV4Response() {
    }

    public AuditEventV4Response(Long auditId, StructuredEvent structuredEvent) {
        this.auditId = auditId;
        this.structuredEvent = structuredEvent;
    }

    public OperationDetails getOperation() {
        return structuredEvent == null ? null : structuredEvent.getOperation();
    }

    public Long getAuditId() {
        return auditId;
    }

    public String getStatus() {
        return structuredEvent == null ? null : structuredEvent.getStatus();
    }

    public Long getDuration() {
        return structuredEvent == null ? null : structuredEvent.getDuration();
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
