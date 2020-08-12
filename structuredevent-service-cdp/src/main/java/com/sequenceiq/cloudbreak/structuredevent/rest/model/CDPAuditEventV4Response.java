package com.sequenceiq.cloudbreak.structuredevent.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.Immutable;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.TransformGetterType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPAuditEventV4Response {

    private Long auditId;

    @TransformGetterType
    private CDPStructuredEvent structuredEvent;

    public CDPAuditEventV4Response() {
    }

    public CDPAuditEventV4Response(Long auditId, CDPStructuredEvent structuredEvent) {
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
