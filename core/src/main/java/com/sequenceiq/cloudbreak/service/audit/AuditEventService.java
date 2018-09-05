package com.sequenceiq.cloudbreak.service.audit;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.comparator.audit.AuditEventComparator;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.structuredevent.db.StructuredEventRepository;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

@Service
public class AuditEventService extends AbstractOrganizationAwareResourceService<StructuredEventEntity> {

    @Inject
    private ConversionService conversionService;

    @Inject
    private StructuredEventRepository structuredEventRepository;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    public AuditEvent getAuditEvent(Long auditId) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        return getAuditEventByOrgId(organizationService.getDefaultOrganizationForUser(user).getId(), auditId);
    }

    public AuditEvent getAuditEventByOrgId(Long organizationId, Long auditId) {
        StructuredEventEntity event = Optional.ofNullable(structuredEventRepository.findByOrgIdAndId(organizationId, auditId))
                .orElseThrow(notFound("StructuredEvent", auditId));
        return conversionService.convert(event, AuditEvent.class);
    }

    public List<AuditEvent> getAuditEventsForOrg(String resourceType, Long resourceId, Organization organization) {
        List<AuditEvent> auditEvents = getEventsForUserWithTypeAndResourceIdByOrg(organization, resourceType, resourceId);
        auditEvents.sort(new AuditEventComparator().reversed());
        return auditEvents;
    }

    public List<AuditEvent> getAuditEventsByOrgId(Long organizationId, String resourceType, Long resourceId, User user) {
        Organization organization = getOrganizationService().get(organizationId, user);
        List<AuditEvent> auditEvents = getEventsForUserWithTypeAndResourceIdByOrg(organization, resourceType, resourceId);
        auditEvents.sort(new AuditEventComparator().reversed());
        return auditEvents;
    }

    private List<AuditEvent> getEventsForUserWithTypeAndResourceIdByOrg(Organization organization, String resourceType, Long resourceId) {
        List<StructuredEventEntity> events = structuredEventRepository.findByOrganizationAndResourceTypeAndResourceId(organization, resourceType, resourceId);
        return events != null ? (List<AuditEvent>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(AuditEvent.class))) : Collections.emptyList();
    }

    @Override
    public OrganizationResourceRepository<StructuredEventEntity, Long> repository() {
        return structuredEventRepository;
    }

    @Override
    public OrganizationResource resource() {
        return OrganizationResource.STRUCTURED_EVENT;
    }

    @Override
    protected void prepareDeletion(StructuredEventEntity resource) {

    }

    @Override
    protected void prepareCreation(StructuredEventEntity resource) {

    }
}
