package com.sequenceiq.cloudbreak.structuredevent.service.audit.rest;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@Component
public class CDPRestAuditEventSender {

    public static final String CDP_REST_AUDIT_EVENT = "CDP_REST_AUDIT_EVENT";

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPRestAuditEventSender.class);

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    public void createEvent(CDPStructuredRestCallEvent structuredEvent) {
        sendAsyncEvent(CDP_REST_AUDIT_EVENT, eventFactory.createEvent(structuredEvent));
        LOGGER.debug("Audit Event is fired for this structured event: {}", structuredEvent);
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }
}
