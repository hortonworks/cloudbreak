package com.sequenceiq.cloudbreak.structuredevent.db;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.domain.StructuredEventEntity;
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
public class LegacyStructuredEventDBService extends AbstractWorkspaceAwareResourceService<StructuredEventEntity> implements StructuredEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyStructuredEventDBService.class);

    @Inject
    private ConversionService conversionService;

    @Inject
    private LegacyStructuredEventRepository legacyStructuredEventRepository;

    @Inject
    private PagingStructuredEventRepository pagingStructuredEventRepository;

    @Inject
    private StackService stackService;

    @Override
    public void create(StructuredEvent structuredEvent) {
        LOGGER.info("Stored StructuredEvent type: {}, payload: {}", structuredEvent.getType(),
                AnonymizerUtil.anonymize(JsonUtil.writeValueAsStringSilent(structuredEvent)));
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
        resource.setAccountId(workspace.getTenant().getName());
        return repository().save(resource);
    }

    public boolean isEnabled() {
        return true;
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForWorkspaceWithType(Workspace workspace, Class<T> eventClass) {
        List<StructuredEventEntity> events = legacyStructuredEventRepository.findByWorkspaceAndEventType(workspace, StructuredEventType.getByClass(eventClass));
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForWorkspaceWithTypeSince(Workspace workspace, Class<T> eventClass, Long since) {
        List<StructuredEventEntity> events = legacyStructuredEventRepository.findByWorkspaceIdAndEventTypeSince(workspace.getId(),
                StructuredEventType.getByClass(eventClass), since);
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(StructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId) {
        List<StructuredEventEntity> events = legacyStructuredEventRepository
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
        return legacyStructuredEventRepository;
    }

    @Override
    protected void prepareDeletion(StructuredEventEntity resource) {

    }

    @Override
    protected void prepareCreation(StructuredEventEntity resource) {

    }

    @Override
    public StructuredEventContainer getStructuredEventsForStack(String name, Long workspaceId) {
        StackView stackView = stackService.getViewByNameInWorkspace(name, workspaceId);
        return getEventsForUserWithResourceId(stackView.getType().getResourceType(), getStackIfAvailable(workspaceId, name, stackView.getType()).getId());
    }

    public StructuredEventEntity findByWorkspaceIdAndId(Long workspaceId, Long id) {
        return legacyStructuredEventRepository.findByWorkspaceIdAndId(workspaceId, id);
    }

    public List<StructuredEventEntity> findByWorkspaceAndResourceTypeAndResourceCrn(Workspace workspace, String resourceType, String resourceCrn) {
        return legacyStructuredEventRepository.findByWorkspaceAndResourceTypeAndResourceCrn(workspace, resourceType, resourceCrn);
    }

    public List<StructuredEventEntity> findByWorkspaceAndResourceTypeAndResourceId(Workspace workspace, String resourceType, Long resourceId) {
        return legacyStructuredEventRepository.findByWorkspaceAndResourceTypeAndResourceId(workspace, resourceType, resourceId);
    }

    private Stack getStackIfAvailable(Long workspaceId, String name, StackType stackType) {
        return Optional.ofNullable(stackService.getByNameInWorkspace(name, workspaceId))
                .orElseThrow(notFound(stackType.getResourceType(), name));
    }
}
