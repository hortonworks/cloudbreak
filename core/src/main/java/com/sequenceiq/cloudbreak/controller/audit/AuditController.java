package com.sequenceiq.cloudbreak.controller.audit;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.AuditEndpoint;
import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.service.audit.AuditEventService;

@Component
@Transactional(TxType.NEVER)
public class AuditController extends BaseAuditController implements AuditEndpoint {

    @Inject
    private AuditEventService auditEventService;

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
        return auditEventService.getAuditEventsForDefaultOrg(resourceType, resourceId);
    }
}
