package com.sequenceiq.cloudbreak.controller.v4;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.requests.GetAuditEventRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.controller.audit.BaseAuditController;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;

@Controller
public class AuditEventV4Controller extends BaseAuditController implements AuditEventV4Endpoint {

    @Inject
    private AuditEventService auditEventService;

    @Override
    public AuditEventV4Response getAuditEventById(Long workspaceId, Long auditId) {
        return auditEventService.getAuditEventByWorkspaceId(workspaceId, auditId);
    }

    @Override
    public AuditEventV4Responses getAuditEvents(Long workspaceId, GetAuditEventRequest getAuditRequest) {
        List<AuditEventV4Response> auditEventsByWorkspaceId = auditEventService.getAuditEventsByWorkspaceId(workspaceId,
                getAuditRequest.getResourceType(), getAuditRequest.getResourceId());
        return new AuditEventV4Responses(auditEventsByWorkspaceId);

    }

    @Override
    public Response getAuditEventsZip(Long workspaceId, GetAuditEventRequest getAuditRequest) {
        List<AuditEventV4Response> auditEvents = getAuditEvents(workspaceId, getAuditRequest).getResponses();
        return getAuditEventsZipResponse(auditEvents, getAuditRequest.getResourceType(), getAuditRequest.getResourceId());
    }
}
