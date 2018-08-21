package com.sequenceiq.cloudbreak.service.audit;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;

public interface AuditEventService extends OrganizationAwareResourceService<StructuredEventEntity> {

    AuditEvent getAuditEventByOrgId(Long organizationId, Long auditId);

    List<AuditEvent> getAuditEventsByOrgId(Long organizationId, String resourceType, Long resourceId);
}
