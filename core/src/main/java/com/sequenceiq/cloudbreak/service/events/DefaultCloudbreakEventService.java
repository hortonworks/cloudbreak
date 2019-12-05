package com.sequenceiq.cloudbreak.service.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.decorator.StackResponseDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredFlowEventFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

@Service
@Transactional
public class DefaultCloudbreakEventService implements CloudbreakEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventService.class);

    private static final String CLOUDBREAK_EVENT = "CLOUDBREAK_EVENT";

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus reactor;

    @Inject
    private CloudbreakEventHandler cloudbreakEventHandler;

    @Inject
    private StructuredEventService structuredEventService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private StackService stackService;

    @Inject
    private StructuredFlowEventFactory structuredFlowEventFactory;

    @Inject
    private StackToStackV4ResponseConverter stackV4ResponseConverter;

    @Inject
    private StackResponseDecorator stackResponseDecorator;

    @PostConstruct
    public void setup() {
        reactor.on(Selectors.$(CLOUDBREAK_EVENT), cloudbreakEventHandler);
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
    public void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String instanceGroupName, ResourceEvent resourceEvent,
            Collection<String> eventMessageArgs) {
        String eventMessage = getMessage(resourceEvent, eventMessageArgs);
        LOGGER.debug("Firing Cloudbreak event: stackId: {}, type: {}, message: {}, instancegroup: {}", stackId, eventType, eventMessage, instanceGroupName);
        fireEventWithPayload(stackId, eventType, resourceEvent, eventMessageArgs, eventMessage, instanceGroupName);
    }

    @Override
    public List<StructuredNotificationEvent> cloudbreakEvents(Long workspaceId, Long since) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(workspaceId, user);
        List<StructuredNotificationEvent> events;
        events = null == since ? structuredEventService.getEventsForWorkspaceWithType(workspace, StructuredNotificationEvent.class)
                : structuredEventService.getEventsForWorkspaceWithTypeSince(workspace, StructuredNotificationEvent.class, since);
        return events;
    }

    @Override
    public List<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId) {
        List<StructuredNotificationEvent> events = new ArrayList<>();
        if (stackId != null) {
            events = structuredEventService.getEventsWithTypeAndResourceId(StructuredNotificationEvent.class, "stacks", stackId);
        }
        return events;
    }

    @Override
    public Page<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId, Pageable pageable) {
        return Optional.ofNullable(stackId)
                .map(id -> structuredEventService.getEventsLimitedWithTypeAndResourceId(StructuredNotificationEvent.class, "stacks", id, pageable))
                .orElse(Page.empty());
    }

    private String getMessage(ResourceEvent resourceEvent, Collection<String> eventMessageArgs) {
        return CollectionUtils.isEmpty(eventMessageArgs)
                ? messagesService.getMessage(resourceEvent.getMessage())
                : messagesService.getMessage(resourceEvent.getMessage(), eventMessageArgs);
    }

    private void fireEventWithPayload(Long stackId, String eventType, ResourceEvent resourceEvent, Collection<String> eventMessageArgs,
            String eventMessage, String instanceGroupName) {

        Stack stack = stackService.getByIdWithTransaction(stackId);
        StructuredNotificationEvent structuredNotificationEvent = structuredFlowEventFactory.createStructuredNotificationEvent(
                stack,
                eventType,
                eventMessage,
                instanceGroupName);

        CloudbreakCompositeEvent compositeEvent = new CloudbreakCompositeEvent(resourceEvent, eventMessageArgs, structuredNotificationEvent);
        if (stackTypeIsDistroX(stack.getType())) {
            StackV4Response stackV4Response = stackV4ResponseConverter.convert(stack);
            stackV4Response = stackResponseDecorator.decorate(stackV4Response, stack, List.of(StackResponseEntries.HARDWARE_INFO.getEntryName()));
            compositeEvent = new CloudbreakCompositeEvent(
                    resourceEvent,
                    eventMessageArgs,
                    structuredNotificationEvent,
                    stackV4Response,
                    stack.getCreator().getUserCrn()
            );
        }
        reactor.notify(CLOUDBREAK_EVENT, eventFactory.createEvent(compositeEvent));
    }

    private boolean stackTypeIsDistroX(StackType stackType) {
        return stackType == null || StackType.WORKLOAD.equals(stackType);
    }
}
