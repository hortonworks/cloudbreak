package com.sequenceiq.cloudbreak.structuredevent.db;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.cloudbreak.util.Benchmark.measureAndWarnIfLong;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyStructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.converter.StructuredEventEntityToStructuredEventConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.converter.StructuredEventToStructuredEventEntityConverter;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Component
public class LegacyStructuredEventDBService extends AbstractWorkspaceAwareResourceService<StructuredEventEntity> implements LegacyStructuredEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyStructuredEventDBService.class);

    private static final long NINETY_DAYS = 90L;

    private static final long THREE_MONTHS = Duration.ofDays(NINETY_DAYS).toMillis();

    private static final int MILLISEC_MULTIPLIER = 1000;

    @Inject
    private LegacyStructuredEventRepository repository;

    @Inject
    private LegacyPagingStructuredEventRepository pagingRepository;

    @Inject
    private StackService stackService;

    @Inject
    private StructuredEventToStructuredEventEntityConverter structuredEventToStructuredEventEntityConverter;

    @Inject
    private StructuredEventEntityToStructuredEventConverter structuredEventEntityToStructuredEventConverter;

    @Override
    public void create(StructuredEvent structuredEvent) {
        StructuredEventEntity structuredEventEntityEntity = structuredEventToStructuredEventEntityConverter.convert(structuredEvent);
        create(structuredEventEntityEntity, structuredEventEntityEntity.getWorkspace(), null);
    }

    @Override
    public StructuredEventEntity create(StructuredEventEntity resource, @Nonnull Long workspaceId, User user) {
        Workspace workspace = getWorkspaceService().getByIdWithoutAuth(workspaceId);
        return create(resource, workspace, user);
    }

    @Override
    public StructuredEventEntity create(StructuredEventEntity resource, Workspace workspace, User user) {
        if (resource != null && resource.getEventType() == NOTIFICATION) {
            resource.setWorkspace(workspace);
            LOGGER.info("Stored StructuredEvent type: {}, payload: {}", resource.getEventType().name(), resource.getStructuredEventJson().getValue());
            return repository().save(resource);
        } else {
            return null;
        }
    }

    public boolean isEnabled() {
        return true;
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForWorkspaceWithType(Workspace workspace, Class<T> eventClass) {
        List<StructuredEventEntity> events = repository.findByWorkspaceAndEventType(workspace, StructuredEventType.getByClass(eventClass));
        return events != null ? (List<T>) events.stream()
                .map(e -> structuredEventEntityToStructuredEventConverter.convert(e))
                .collect(Collectors.toList()) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsForWorkspaceWithTypeSince(Workspace workspace, Class<T> eventClass, Long since) {
        List<StructuredEventEntity> events = repository.findByWorkspaceIdAndEventTypeSince(workspace.getId(),
                StructuredEventType.getByClass(eventClass), since);
        return events != null ? (List<T>) events.stream()
                .map(e -> structuredEventEntityToStructuredEventConverter.convert(e))
                .collect(Collectors.toList()) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> List<T> getEventsWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId) {
        List<StructuredEventEntity> events = repository
                .findByEventTypeAndResourceTypeAndResourceId(StructuredEventType.getByClass(eventClass), resourceType, resourceId);
        return events != null ? (List<T>) events.stream()
                .map(e -> structuredEventEntityToStructuredEventConverter.convert(e))
                .collect(Collectors.toList()) : Collections.emptyList();
    }

    @Override
    public <T extends StructuredEvent> Page<T> getEventsLimitedWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId,
            Pageable pageable) {
        Page<StructuredEventEntity> events = pagingRepository.findByEventTypeAndResourceTypeAndResourceId(StructuredEventType.getByClass(eventClass),
                resourceType, resourceId, pageable);
        return (Page<T>) Optional.ofNullable(events).orElse(Page.empty()).map(event -> structuredEventEntityToStructuredEventConverter.convert(event));
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
        return repository;
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

    @Override
    public StructuredEventContainer getStructuredEventsForStackByCrn(String crn, Long workspaceId, boolean onlyAlive) {
        Stack stack;
        if (onlyAlive) {
            stack = getStackIfAvailableByCrn(workspaceId, crn);
        } else {
            stack = getStackByCrn(workspaceId, crn);
        }
        return getEventsForUserWithResourceId(stack.getType().getResourceType(), stack.getId());
    }

    public void deleteEntriesByResourceIdsOlderThanThreeMonths(Long resourceId) {
        measureAndWarnIfLong(() -> repository.deleteRecordsByResourceIdOlderThan(resourceId, getTimestampThatThreeMonthsBeforeNow()), LOGGER,
                "Cleaning up StructuredEvent(s) (for resourceId: " + resourceId + ") that are older than 3 months");
    }

    public StructuredEventEntity findByWorkspaceIdAndId(Long workspaceId, Long id) {
        LOGGER.debug("Looking up for {} entry in DB based on the workspace ID [{}] and resource ID [{}]", StructuredEventEntity.class.getSimpleName(),
                workspaceId, id);
        return repository.findByWorkspaceIdAndId(workspaceId, id);
    }

    public List<StructuredEventEntity> findByWorkspaceAndResourceTypeAndResourceCrn(Workspace workspace, String resourceCrn) {
        return repository.findByWorkspaceAndResourceCrn(workspace, resourceCrn);
    }

    public List<StructuredEventEntity> findByWorkspaceAndResourceTypeAndResourceId(Workspace workspace, String resourceType, Long resourceId) {
        return repository.findByWorkspaceAndResourceTypeAndResourceId(workspace, resourceType, resourceId);
    }

    private Stack getStackIfAvailable(Long workspaceId, String name, StackType stackType) {
        return Optional.ofNullable(stackService.getByNameInWorkspace(name, workspaceId))
                .orElseThrow(notFound(stackType.getResourceType(), name));
    }

    private Stack getStackIfAvailableByCrn(Long workspaceId, String crn) {
        return Optional.ofNullable(stackService.getNotTerminatedByCrnInWorkspace(crn, workspaceId))
                .orElseThrow(notFound(Crn.safeFromString(crn).getResourceType().toString(), crn));
    }

    private Stack getStackByCrn(Long workspaceId, String crn) {
        return Optional.ofNullable(stackService.getByCrnInWorkspace(crn, workspaceId))
                .orElseThrow(notFound(Crn.safeFromString(crn).getResourceType().toString(), crn));
    }

    private Long getTimestampThatThreeMonthsBeforeNow() {
        return Instant.now().getEpochSecond() * MILLISEC_MULTIPLIER - THREE_MONTHS;
    }

}
