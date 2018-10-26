package com.sequenceiq.cloudbreak.controller.audit;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.AuditEndpoint;
import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class AuditController extends BaseAuditController implements AuditEndpoint {

    @Inject
    private AuditEventService auditEventService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public AuditEvent getAuditEvent(Long auditId) {
        return auditEventService.getAuditEvent(auditId);
    }

    @Override
    public Response getAuditEventsZip(String resourceType, Long resourceId) {
        List<AuditEvent> auditEvents = getAuditEvents(resourceType, resourceId);
        return getAuditEventsZipResponse(auditEvents, resourceType, resourceId);
    }

    @Override
    public List<AuditEvent> getAuditEvents(String resourceType, Long resourceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return auditEventService.getAuditEventsForWorkspace(resourceType, resourceId, workspace);
    }
}
