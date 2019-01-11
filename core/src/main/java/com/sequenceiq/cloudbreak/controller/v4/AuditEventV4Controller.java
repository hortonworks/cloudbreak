package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses.auditEventV4Responses;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.requests.GetAuditEventRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralListV4Response;
import com.sequenceiq.cloudbreak.controller.audit.BaseAuditController;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;

@Controller
public class AuditEventV4Controller extends BaseAuditController implements AuditEventV4Endpoint {

    @Inject
    private AuditEventService auditEventService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public AuditEventV4Response getAuditEventById(Long workspaceId, Long auditId) {
        return auditEventService.getAuditEventByWorkspaceId(workspaceId, auditId);
    }

    @Override
    public GeneralListV4Response<AuditEventV4Response> getAuditEvents(Long workspaceId, GetAuditEventRequest getAuditRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        List<AuditEventV4Response> auditEventsByWorkspaceId = auditEventService.getAuditEventsByWorkspaceId(workspaceId, getAuditRequest.getResourceType(), getAuditRequest.getResourceId(), user);
        return GeneralListV4Response.propagateResponses(auditEventsByWorkspaceId);

    }

    @Override
    public Response getAuditEventsZip(Long workspaceId, GetAuditEventRequest getAuditRequest) {
        List<AuditEventV4Response> auditEvents = getAuditEvents(workspaceId, getAuditRequest).getResponses();
        return getAuditEventsZipResponse(auditEvents, getAuditRequest.getResourceType(), getAuditRequest.getResourceId());
    }
}
