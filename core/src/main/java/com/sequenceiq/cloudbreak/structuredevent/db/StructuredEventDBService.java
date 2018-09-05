package com.sequenceiq.cloudbreak.structuredevent.db;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;

@Component
public class StructuredEventDBService extends AbstractOrganizationAwareResourceService<StructuredEventEntity> implements StructuredEventService {

    @Inject
    private ConversionService conversionService;

    @Inject
    private StructuredEventRepository structuredEventRepository;

    @Override
    public void storeStructuredEvent(StructuredEvent structuredEvent) {
        StructuredEventEntity structuredEventEntityEntity = conversionService.convert(structuredEvent, StructuredEventEntity.class);
        create(structuredEventEntityEntity, structuredEventEntityEntity.getOrganization(), null);
    }

    @Override
    public StructuredEventEntity create(StructuredEventEntity resource, @Nonnull Long organizationId, User user) {
        Organization organization = getOrganizationService().getById(organizationId);
        return create(resource, organization, user);
    }

    @Override
    public StructuredEventEntity create(StructuredEventEntity resource, Organization organization, User user) {
        resource.setOrganization(organization);
        return repository().save(resource);
    }

    public boolean isEnabled() {
        return true;
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForOrgWithType(Organization organization, Class<T> eventClass) {
        List<StructuredEventEntity> events = structuredEventRepository.findByOrganizationAndEventType(organization, StructuredEventType.getByClass(eventClass));
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForOrgWithTypeSince(Organization organization, Class<T> eventClass, Long since) {
        List<StructuredEventEntity> events = structuredEventRepository.findByOrgIdAndEventTypeSince(organization.getId(),
                StructuredEventType.getByClass(eventClass), since);
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId) {
        List<StructuredEventEntity> events = structuredEventRepository
                .findByEventTypeAndResourceTypeAndResourceId(StructuredEventType.getByClass(eventClass), resourceType, resourceId);
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public StructuredEventContainer getEventsForUserWithResourceId(String resourceType, Long resourceId) {
        List<StructuredRestCallEvent> rest = getEventsWithTypeAndResourceId(StructuredRestCallEvent.class, resourceType, resourceId);
        List<StructuredFlowEvent> flow = getEventsWithTypeAndResourceId(StructuredFlowEvent.class, resourceType, resourceId);
        List<StructuredNotificationEvent> notification
                = getEventsWithTypeAndResourceId(StructuredNotificationEvent.class, resourceType, resourceId);
        return new StructuredEventContainer(flow, rest, notification);
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
