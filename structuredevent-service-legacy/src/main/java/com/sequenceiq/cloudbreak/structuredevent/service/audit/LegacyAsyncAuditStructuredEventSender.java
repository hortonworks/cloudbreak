package com.sequenceiq.cloudbreak.structuredevent.service.audit;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.StructuredEventSenderService;
import com.sequenceiq.cloudbreak.structuredevent.conf.StructuredEventEnablementConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class LegacyAsyncAuditStructuredEventSender implements StructuredEventSenderService {

    public static final String AUDIT_EVENT_LOG_MESSAGE = "AUDIT_EVENT_LOG_MESSAGE";

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyAsyncAuditStructuredEventSender.class);

    @Inject
    private StructuredEventEnablementConfig structuredEventSenderConfig;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Inject
    private Map<String, LegacyEventDataExtractor<? extends StructuredEvent>> eventDataExtractorMap;

    @Override
    public boolean isEnabled() {
        return structuredEventSenderConfig.isAuditServiceEnabled();
    }

    @Override
    public void create(StructuredEvent structuredEvent) {
        String eventType = structuredEvent.getOperation().getEventType().name();
        LegacyEventDataExtractor<? extends StructuredEvent> eventDataExtractor = eventDataExtractorMap.get(eventType.toLowerCase() + "LegacyEventDataExtractor");
        if (eventDataExtractor == null) {
            LOGGER.debug("Event data converter does not exist for event type of {}", eventType);
            return;
        }
        boolean shouldAudit = eventDataExtractor.shouldAudit(structuredEvent);
        if (shouldAudit) {
            sendAsyncEvent(AUDIT_EVENT_LOG_MESSAGE, eventFactory.createEvent(structuredEvent));
        } else {
            LOGGER.debug("Audit Event is disabled for this structure event: {}", structuredEvent);
        }
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }
}
