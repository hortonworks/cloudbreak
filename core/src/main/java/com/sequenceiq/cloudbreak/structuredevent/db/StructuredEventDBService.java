package com.sequenceiq.cloudbreak.structuredevent.db;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Component
public class StructuredEventDBService extends AbstractWorkspaceAwareResourceService<StructuredEventEntity> implements StructuredEventService {

    @Inject
    private ConversionService conversionService;

    @Inject
    private StructuredEventRepository structuredEventRepository;

    @Inject
    private PagingStructuredEventRepository pagingStructuredEventRepository;

    @Inject
    private StackService stackService;

    @Override
    public void storeStructuredEvent(StructuredEvent structuredEvent) {
        StructuredEventEntity structuredEventEntityEntity = conversionService.convert(structuredEvent, StructuredEventEntity.class);
        create(structuredEventEntityEntity, structuredEventEntityEntity.getWorkspace(), null);
    }

    @Override
    public StructuredEventEntity create(StructuredEventEntity resource, @Nonnull Long workspaceId, User user) {
        Workspace workspace = getWorkspaceService().getByIdWithoutAuth(workspaceId);
        return create(resource, workspace, user);
    }

    @Override
    public StructuredEventEntity create(StructuredEventEntity resource, Workspace workspace, User user) {
        resource.setWorkspace(workspace);
        return repository().save(resource);
    }

    public boolean isEnabled() {
        return true;
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForWorkspaceWithType(Workspace workspace, Class<T> eventClass) {
        List<StructuredEventEntity> events = structuredEventRepository.findByWorkspaceAndEventType(workspace, StructuredEventType.getByClass(eventClass));
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForWorkspaceWithTypeSince(Workspace workspace, Class<T> eventClass, Long since) {
        List<StructuredEventEntity> events = structuredEventRepository.findByWorkspaceIdAndEventTypeSince(workspace.getId(),
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
    public <T extends StructuredEvent> Page<T> getEventsLimitedWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId,
            Pageable pageable) {
        Page<StructuredEventEntity> events = pagingStructuredEventRepository
                .findByEventTypeAndResourceTypeAndResourceId(StructuredEventType.getByClass(eventClass), resourceType, resourceId, pageable);
        return (Page<T>) Optional.ofNullable(events).orElse(Page.empty()).map(event -> conversionService.convert(event, StructuredEvent.class));
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
    public WorkspaceResourceRepository<StructuredEventEntity, Long> repository() {
        return structuredEventRepository;
    }

    @Override
    protected void prepareDeletion(StructuredEventEntity resource) {

    }

    @Override
    protected void prepareCreation(StructuredEventEntity resource) {

    }

    @Override
    public StructuredEventContainer getStructuredEventsForStack(String name, Long workspaceId) {
        return getEventsForUserWithResourceId("stacks", getStackIfAvailable(workspaceId, name).getId());
    }

    public StructuredEventEntity findByWorkspaceIdAndId(Long workspaceId, Long id) {
        return structuredEventRepository.findByWorkspaceIdAndId(workspaceId, id);
    }

    public List<StructuredEventEntity> findByWorkspaceAndResourceTypeAndResourceCrn(Workspace workspace, String resourceType, String resourceCrn) {
        return structuredEventRepository.findByWorkspaceAndResourceTypeAndResourceCrn(workspace, resourceType, resourceCrn);
    }

    public List<StructuredEventEntity> findByWorkspaceAndResourceTypeAndResourceId(Workspace workspace, String resourceType, Long resourceId) {
        return structuredEventRepository.findByWorkspaceAndResourceTypeAndResourceId(workspace, resourceType, resourceId);
    }

    private Stack getStackIfAvailable(Long workspaceId, String name) {
        return Optional.ofNullable(stackService.getByNameInWorkspace(name, workspaceId)).orElseThrow(notFound("stack", name));
    }
}
