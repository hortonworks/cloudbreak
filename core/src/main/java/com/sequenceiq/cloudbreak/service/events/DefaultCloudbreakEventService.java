package com.sequenceiq.cloudbreak.service.events;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.StructuredFlowEventFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

@Service
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
    public List<StructuredNotificationEvent> cloudbreakEvents(Organization organization, Long since) {
        List<StructuredNotificationEvent> events;
        events = null == since ? structuredEventService.getEventsForOrgWithType(organization, StructuredNotificationEvent.class)
                : structuredEventService.getEventsForOrgWithTypeSince(organization, StructuredNotificationEvent.class, since);
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
}
