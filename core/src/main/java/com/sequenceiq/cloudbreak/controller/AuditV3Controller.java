package com.sequenceiq.cloudbreak.controller;

import java.util.List;

import javax.transaction.Transactional;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.AuditV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class AuditV3Controller implements AuditV3Endpoint {

    @Override
    public AuditEvent getAuditEventByOrganization(Long organizationId, Long auditId) {
        return null;
    }

    @Override
    public List<AuditEvent> getAuditEventsInOrganization(Long organizationId, String resourceType, Long resourceId) {
        return null;
    }

    @Override
    public Response getAuditEventsZipInOrganization(Long organizationId, String resourceType, Long resourceId) {
        return null;
    }
}
