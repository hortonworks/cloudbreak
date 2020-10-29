package com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.common.model.annotations.Immutable;
import com.sequenceiq.common.model.annotations.TransformGetterType;

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

    public Long getAuditId() {
        return auditId;
    }

    public StructuredEvent getStructuredEvent() {
        return structuredEvent;
    }

}
