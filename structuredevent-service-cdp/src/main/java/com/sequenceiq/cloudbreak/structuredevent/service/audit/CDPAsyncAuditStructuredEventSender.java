package com.sequenceiq.cloudbreak.structuredevent.service.audit;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.conf.StructuredEventEnablementConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventSenderService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CDPAsyncAuditStructuredEventSender implements CDPStructuredEventSenderService {

    public static final String AUDIT_EVENT_LOG_MESSAGE = "AUDIT_EVENT_LOG_MESSAGE";

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPAsyncAuditStructuredEventSender.class);

    @Inject
    private StructuredEventEnablementConfig structuredEventSenderConfig;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Inject
    private Map<String, CDPEventDataExtractor<? extends CDPStructuredEvent>> eventDataExtractorMap;

    @Override
    public boolean isEnabled() {
        return structuredEventSenderConfig.isAuditServiceEnabled();
    }

    @Override
    public void create(CDPStructuredEvent structuredEvent) {
        StructuredEventType eventType = structuredEvent.getOperation().getEventType();
        CDPEventDataExtractor<? extends CDPStructuredEvent> eventDataExtractor =
                eventDataExtractorMap.get(eventType.name().toLowerCase() + "CDPEventDataExtractor");
        if (eventDataExtractor == null) {
            LOGGER.debug("Event data converter does not exist for event type of {}", eventType);
            return;
        }
        boolean shouldAudit = eventDataExtractor.shouldAudit(structuredEvent);
        if (shouldAudit) {
            sendAsyncEvent(AUDIT_EVENT_LOG_MESSAGE, eventFactory.createEvent(structuredEvent));
            LOGGER.debug("Audit Event is fired for this structured event: {}", structuredEvent);
        } else {
            LOGGER.debug("Audit Event is disabled for this structured event: {}", structuredEvent);
        }
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }
}
