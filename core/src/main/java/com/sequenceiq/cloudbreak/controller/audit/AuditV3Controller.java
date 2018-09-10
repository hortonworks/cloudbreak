package com.sequenceiq.cloudbreak.controller.audit;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.AuditV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@Transactional(TxType.NEVER)
public class AuditV3Controller extends BaseAuditController implements AuditV3Endpoint {

    @Inject
    private AuditEventService auditEventService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public AuditEvent getAuditEventByWorkspace(Long workspaceId, Long auditId) {
        return auditEventService.getAuditEventByWorkspaceId(workspaceId, auditId);
    }

    @Override
    public List<AuditEvent> getAuditEventsInWorkspace(Long workspaceId, String resourceType, Long resourceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        return auditEventService.getAuditEventsByWorkspaceId(workspaceId, resourceType, resourceId, user);
    }

    @Override
    public Response getAuditEventsZipInWorkspace(Long workspaceId, String resourceType, Long resourceId) {
        List<AuditEvent> auditEvents = getAuditEventsInWorkspace(workspaceId, resourceType, resourceId);
        return getAuditEventsZipResponse(auditEvents, resourceType, resourceId);
    }
}
