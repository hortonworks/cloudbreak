package com.sequenceiq.cloudbreak.service.events;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredFlowEventFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

@Service
public class DefaultCloudbreakEventService implements CloudbreakEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventService.class);

    private static final String CLOUDBREAK_EVENT = "CLOUDBREAK_EVENT";

    private static final Integer NOTIFICATION_EVEN_LIMIT = 100;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus reactor;

    @Inject
    private CloudbreakEventHandler cloudbreakEventHandler;

    @Inject
    private StructuredFlowEventFactory structuredFlowEventFactory;

    @Inject
    private StructuredEventService structuredEventService;

    @PostConstruct
    public void setup() {
        reactor.on(Selectors.$(CLOUDBREAK_EVENT), cloudbreakEventHandler);
    }

    @Override
    public void fireCloudbreakEvent(Long entityId, String eventType, String eventMessage) {
        StructuredNotificationEvent eventData = structuredFlowEventFactory.createStructuredNotificationEvent(entityId, eventType, eventMessage, null);
        LOGGER.info("Firing Cloudbreak event: entityId: {}, type: {}, message: {}", entityId, eventType, eventMessage);
        reactor.notify(CLOUDBREAK_EVENT, eventFactory.createEvent(eventData));
    }

    @Override
    public void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String eventMessage, String instanceGroupName) {
        StructuredNotificationEvent eventData = structuredFlowEventFactory.createStructuredNotificationEvent(stackId, eventType, eventMessage,
                instanceGroupName);
        LOGGER.info("Firing Cloudbreak event: stackId: {}, type: {}, message: {}, instancegroup: {}", stackId, eventType, eventMessage, instanceGroupName);
        reactor.notify(CLOUDBREAK_EVENT, eventFactory.createEvent(eventData));
    }

    @Override
    public List<StructuredNotificationEvent> cloudbreakEvents(Workspace workspace, Long since) {
        List<StructuredNotificationEvent> events;
        events = null == since ? structuredEventService.getEventsForWorkspaceWithType(workspace, StructuredNotificationEvent.class)
                : structuredEventService.getEventsForWorkspaceWithTypeSince(workspace, StructuredNotificationEvent.class, since);
        return events;
    }

    @Override
    public Page<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId, Pageable pageable) {
        return Optional.ofNullable(stackId)
                .map(id -> structuredEventService.getEventsLimitedWithTypeAndResourceId(StructuredNotificationEvent.class, "stacks", id, pageable))
                .orElse(Page.empty());
    }
}
