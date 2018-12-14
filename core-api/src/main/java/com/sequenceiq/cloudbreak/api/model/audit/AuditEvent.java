package com.sequenceiq.cloudbreak.api.model.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.annotations.Immutable;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditEvent {

    private Long auditId;

    private StructuredEventResponse structuredEvent;

    public AuditEvent(Long auditId, StructuredEventResponse structuredEvent) {
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

    public StructuredEventResponse getStructuredEvent() {
        return structuredEvent;
    }
}
