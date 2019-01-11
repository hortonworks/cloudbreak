package com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditEventV4Responses {

    private List<AuditEventV4Response> auditEvents = new ArrayList<>();

    public List<AuditEventV4Response> getAuditEvents() {
        return auditEvents;
    }

    public void setAuditEvents(List<AuditEventV4Response> auditEvents) {
        this.auditEvents = auditEvents;
    }

    public static final AuditEventV4Responses auditEventV4Responses(List<AuditEventV4Response> auditEvents) {
        AuditEventV4Responses auditEventV4Responses = new AuditEventV4Responses();
        auditEventV4Responses.setAuditEvents(auditEvents);
        return auditEventV4Responses;
    }
}
