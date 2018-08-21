package com.sequenceiq.cloudbreak.service.audit;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.service.organization.LegacyOrganizationAwareResourceService;

public interface LegacyAuditEventService extends LegacyOrganizationAwareResourceService<StructuredEventEntity>, AuditEventService {

    AuditEvent getAuditEvent(Long auditId);

    List<AuditEvent> getAuditEventsForDefaultOrg(String resourceType, Long resourceId);
}
