package com.sequenceiq.it.cloudbreak.context;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;

public class Clue {

    private final String name;

    private final AuditEventV4Responses auditEvents;

    private final Object response;

    private final boolean hasSpotTermination;

    public Clue(String name, AuditEventV4Responses auditEvents, Object response, boolean hasSpotTermination) {
        this.name = name;
        this.auditEvents = auditEvents;
        this.response = response;
        this.hasSpotTermination = hasSpotTermination;
    }

    public String getName() {
        return name;
    }

    public AuditEventV4Responses getAuditEvents() {
        return auditEvents;
    }

    public Object getResponse() {
        return response;
    }

    public boolean isHasSpotTermination() {
        return hasSpotTermination;
    }
}
