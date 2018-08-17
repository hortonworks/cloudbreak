package com.sequenceiq.cloudbreak.controller.audit;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.AuditV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class AuditV3Controller extends BaseAuditController implements AuditV3Endpoint {

    @Inject
    private AuditEventService auditEventService;

    @Override
    public AuditEvent getAuditEventByOrganization(Long organizationId, Long auditId) {
        return auditEventService.getAuditEventByOrgId(organizationId, auditId);
    }

    @Override
    public List<AuditEvent> getAuditEventsInOrganization(Long organizationId, String resourceType, Long resourceId) {
        return auditEventService.getAuditEventsByOrgId(organizationId, resourceType, resourceId);
    }

    @Override
    public Response getAuditEventsZipInOrganization(Long organizationId, String resourceType, Long resourceId) {
        List<AuditEvent> auditEvents = getAuditEventsInOrganization(organizationId, resourceType, resourceId);
        return getAuditEventsZipResponse(auditEvents, resourceType, resourceId);
    }
}
