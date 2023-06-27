package com.sequenceiq.it.cloudbreak.context;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

public class Clue {

    private final String name;

    private final String crn;

    private final AuditEventV4Responses auditEvents;

    private final List<CDPStructuredEvent> cdpStructuredEvents;

    private final Object response;

    private final boolean hasSpotTermination;

    private final String storageUrl;

    private final String searchUrl;

    public Clue(String name,
            String crn,
            String storageUrl,
            String searchUrl,
            AuditEventV4Responses auditEvents,
            List<CDPStructuredEvent> cdpStructuredEvents,
            Object response,
            boolean hasSpotTermination) {
        this.name = name;
        this.crn = crn;
        this.storageUrl = storageUrl;
        this.searchUrl = searchUrl;
        this.auditEvents = auditEvents;
        this.cdpStructuredEvents = cdpStructuredEvents;
        this.response = response;
        this.hasSpotTermination = hasSpotTermination;
    }

    public String getName() {
        return name;
    }

    public String getCrn() {
        return crn;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public AuditEventV4Responses getAuditEvents() {
        return auditEvents;
    }

    public List<CDPStructuredEvent> getCdpStructuredEvents() {
        return cdpStructuredEvents;
    }

    public Object getResponse() {
        return response;
    }

    public boolean isHasSpotTermination() {
        return hasSpotTermination;
    }
}
