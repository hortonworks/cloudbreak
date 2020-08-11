package com.sequenceiq.cloudbreak.structuredevent.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.common.model.annotations.Immutable;
import com.sequenceiq.common.model.annotations.TransformGetterType;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPAuditEventV1Response {

    private Long auditId;

    @TransformGetterType
    private CDPStructuredEvent structuredEvent;

    public CDPAuditEventV1Response() {
    }

    public CDPAuditEventV1Response(Long auditId, CDPStructuredEvent structuredEvent) {
        this.auditId = auditId;
        this.structuredEvent = structuredEvent;
    }

    public CDPOperationDetails getOperation() {
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

    public CDPStructuredEvent getStructuredEvent() {
        return structuredEvent;
    }

    public CDPStructuredFlowEvent getRawFlowEvent() {
        return structuredEvent instanceof CDPStructuredFlowEvent ? (CDPStructuredFlowEvent) structuredEvent : null;
    }

    public CDPStructuredRestCallEvent getRawRestEvent() {
        return structuredEvent instanceof CDPStructuredRestCallEvent ? (CDPStructuredRestCallEvent) structuredEvent : null;
    }

    public CDPStructuredNotificationEvent getRawNotification() {
        return structuredEvent instanceof CDPStructuredNotificationEvent ? (CDPStructuredNotificationEvent) structuredEvent : null;
    }
}
