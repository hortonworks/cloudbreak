package com.sequenceiq.cloudbreak.service.events;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.stack.CmCommandLinkProvider;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.BaseLegacyStructuredFlowEventFactory;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyStructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@Service
public class DefaultCloudbreakEventService implements CloudbreakEventService, ClusterEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventService.class);

    private static final String CLOUDBREAK_EVENT = "CLOUDBREAK_EVENT";

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus reactor;

    @Inject
    private CloudbreakEventHandler cloudbreakEventHandler;

    @Inject
    private LegacyStructuredEventService legacyStructuredEventService;

    @Inject
    private UserService userService;

    @Inject
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private BaseLegacyStructuredFlowEventFactory baseLegacyStructuredFlowEventFactory;

    @Inject
    private StackToStackV4ResponseConverter stackV4ResponseConverter;

    @Inject
    private StackResponseDecorator stackResponseDecorator;

    @Inject
    private CmCommandLinkProvider cmCommandLinkProvider;

    @PostConstruct
    public void setup() {
        reactor.on(CLOUDBREAK_EVENT, cloudbreakEventHandler);
    }

    @Override
    public void fireCloudbreakEvent(Long entityId, String eventType, ResourceEvent resourceEvent) {
        fireCloudbreakEvent(entityId, eventType, resourceEvent, null);
    }

    @Override
    public void fireCloudbreakEvent(Long stackId, String eventType, ResourceEvent resourceEvent, Collection<String> eventMessageArgs) {
        String eventMessage = getMessage(resourceEvent, eventMessageArgs);
        LOGGER.debug("Firing Cloudbreak event: stackId: {}, type: {}, message: {}", stackId, eventType, eventMessage);
        fireEventWithPayload(stackId, eventType, resourceEvent, eventMessageArgs, eventMessage, null);
    }

    @Override
    public void fireCloudbreakEvent(StackDtoDelegate stack, ResourceEvent resourceEvent, Collection<String> eventMessageArgs) {
        String eventMessage = getMessage(resourceEvent, eventMessageArgs);
        LOGGER.debug("Firing Cloudbreak event: stackId: {}, type: {}, message: {}", stack.getId(), stack.getStatus().name(), eventMessage);
        fireEventWithPayload(stack, stack.getStatus().name(), resourceEvent, eventMessageArgs, eventMessage, null);
    }

    @Override
    public void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String instanceGroupName, ResourceEvent resourceEvent,
            Collection<String> eventMessageArgs) {
        String eventMessage = getMessage(resourceEvent, eventMessageArgs);
        LOGGER.debug("Firing Cloudbreak event: stackId: {}, type: {}, message: {}, instancegroup: {}", stackId, eventType, eventMessage, instanceGroupName);
        fireEventWithPayload(stackId, eventType, resourceEvent, eventMessageArgs, eventMessage, instanceGroupName);
    }

    @Override
    public void fireClusterManagerEvent(StackDtoDelegate stack, ResourceEvent resourceEvent, String eventName, Optional<BigDecimal> clusterManagerEventId) {
        if (stack != null && clusterManagerEventId.isPresent()) {
            Optional<String> link = cmCommandLinkProvider.getCmCommandLink(stack, clusterManagerEventId.get().toString());
            if (link.isPresent()) {
                Collection<String> eventMessageArgs = List.of(eventName, link.get());
                String eventMessage = getMessage(resourceEvent, eventMessageArgs);
                LOGGER.debug("Firing Cluster Manager event: stackId: {}, type: {}, message: {}", stack.getId(), stack.getStatus().name(), eventMessage);
                fireEventWithPayload(stack.getId(), stack.getStatus().name(), resourceEvent, eventMessageArgs, eventMessage, null);
            }
        }
    }

    @Override
    public List<StructuredNotificationEvent> cloudbreakEvents(Long workspaceId, Long since) {
        User user = userService.getOrCreate(legacyRestRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(workspaceId, user);
        List<StructuredNotificationEvent> events;
        events = null == since ? legacyStructuredEventService.getEventsForWorkspaceWithType(workspace, StructuredNotificationEvent.class)
                : legacyStructuredEventService.getEventsForWorkspaceWithTypeSince(workspace, StructuredNotificationEvent.class, since);
        return events;
    }

    @Override
    public Page<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId, String resourceType, Pageable pageable) {
        return Optional.ofNullable(stackId)
                .map(id -> legacyStructuredEventService.getEventsLimitedWithTypeAndResourceId(StructuredNotificationEvent.class, resourceType, id, pageable))
                .orElse(Page.empty());
    }

    @Override
    public List<StructuredNotificationEvent> cloudbreakLastEventsForStack(Long stackId, String stackType, int size) {
        return Optional.ofNullable(stackId)
                .map(id -> legacyStructuredEventService.getLastEventsWithTypeAndResourceId(StructuredNotificationEvent.class, stackType, id, size))
                .orElse(new ArrayList<>());
    }

    private String getMessage(ResourceEvent resourceEvent, Collection<String> eventMessageArgs) {
        return CollectionUtils.isEmpty(eventMessageArgs)
                ? messagesService.getMessage(resourceEvent.getMessage())
                : messagesService.getMessage(resourceEvent.getMessage(), eventMessageArgs);
    }

    private void fireEventWithPayload(Long stackId, String eventType, ResourceEvent resourceEvent, Collection<String> eventMessageArgs,
            String eventMessage, String instanceGroupName) {
        StackDto stack = stackDtoService.getByIdWithoutResources(stackId);
        fireEventWithPayload(stack, eventType, resourceEvent, eventMessageArgs, eventMessage, instanceGroupName);
    }

    private void fireEventWithPayload(StackDtoDelegate stack, String eventType, ResourceEvent resourceEvent, Collection<String> eventMessageArgs,
            String eventMessage,
            String instanceGroupName) {
        StructuredNotificationEvent structuredNotificationEvent = baseLegacyStructuredFlowEventFactory.createStructuredNotificationEvent(
                stack,
                eventType,
                eventMessage,
                instanceGroupName);

        CloudbreakCompositeEvent compositeEvent;
        try {
            StackV4Response stackV4Response = stackV4ResponseConverter.convert(stack);
            stackV4Response = stackResponseDecorator.decorate(stackV4Response, stack, List.of(StackResponseEntries.HARDWARE_INFO.getEntryName()));
            compositeEvent = new CloudbreakCompositeEvent(
                    resourceEvent,
                    eventMessageArgs,
                    structuredNotificationEvent,
                    stackV4Response,
                    stack.getCreator().getUserCrn()
            );
        } catch (Exception e) {
            LOGGER.error("Failed to convert stack to response for event", e);
            compositeEvent = new CloudbreakCompositeEvent(
                    resourceEvent,
                    eventMessageArgs,
                    structuredNotificationEvent
            );
        }

        reactor.notify(CLOUDBREAK_EVENT, eventFactory.createEvent(compositeEvent));
    }

}
