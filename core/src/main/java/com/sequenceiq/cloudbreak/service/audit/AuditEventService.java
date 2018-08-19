package com.sequenceiq.cloudbreak.service.audit;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.comparator.audit.AuditEventComparator;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.structuredevent.db.StructuredEventRepository;

@Service
public class AuditEventService extends AbstractOrganizationAwareResourceService<StructuredEventEntity> {

    @Inject
    private ConversionService conversionService;

    @Inject
    private StructuredEventRepository structuredEventRepository;

    public AuditEvent getAuditEvent(Long auditId) {
        Optional<StructuredEventEntity> event = structuredEventRepository.findById(auditId);
        return event.isPresent() ? conversionService.convert(event, AuditEvent.class) : null;
    }

    public AuditEvent getAuditEventByOrgId(Long organizationId, Long auditId) {
        StructuredEventEntity event = structuredEventRepository.findByOrgIdAndId(organizationId, auditId);
        return event != null ? conversionService.convert(event, AuditEvent.class) : null;
    }

    public List<AuditEvent> getAuditEventsForDefaultOrg(String resourceType, Long resourceId) {
        Organization organization = getOrganizationService().getDefaultOrganizationForCurrentUser();
        List<AuditEvent> auditEvents = getEventsForUserWithTypeAndResourceIdByOrg(organization, resourceType, resourceId);
        Collections.sort(auditEvents, new AuditEventComparator().reversed());
        return auditEvents;
    }

    public List<AuditEvent> getAuditEventsByOrgId(Long organizationId, String resourceType, Long resourceId) {
        Organization organization = getOrganizationService().get(organizationId);
        List<AuditEvent> auditEvents = getEventsForUserWithTypeAndResourceIdByOrg(organization, resourceType, resourceId);
        Collections.sort(auditEvents, new AuditEventComparator().reversed());
        return auditEvents;
    }

    private List<AuditEvent> getEventsForUserWithTypeAndResourceIdByOrg(Organization organization, String resourceType, Long resourceId) {
        List<StructuredEventEntity> events = structuredEventRepository.findByOrganizationAndResourceTypeAndResourceId(organization, resourceType, resourceId);
        return events != null ? (List<AuditEvent>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(AuditEvent.class))) : Collections.emptyList();
    }

    @Override
    protected OrganizationResourceRepository<StructuredEventEntity, Long> repository() {
        return structuredEventRepository;
    }

    @Override
    protected OrganizationResource resource() {
        return OrganizationResource.STRUCTURED_EVENT;
    }

    @Override
    protected void prepareDeletion(StructuredEventEntity resource) {

    }

    @Override
    protected void prepareCreation(StructuredEventEntity resource) {

    }
}
