package com.sequenceiq.cloudbreak.controller.audit;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.AuditV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.domain.organization.User;
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
    public AuditEvent getAuditEventByOrganization(Long organizationId, Long auditId) {
        return auditEventService.getAuditEventByOrgId(organizationId, auditId);
    }

    @Override
    public List<AuditEvent> getAuditEventsInOrganization(Long organizationId, String resourceType, Long resourceId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        return auditEventService.getAuditEventsByOrgId(organizationId, resourceType, resourceId, user);
    }

    @Override
    public Response getAuditEventsZipInOrganization(Long organizationId, String resourceType, Long resourceId) {
        List<AuditEvent> auditEvents = getAuditEventsInOrganization(organizationId, resourceType, resourceId);
        return getAuditEventsZipResponse(auditEvents, resourceType, resourceId);
    }
}
