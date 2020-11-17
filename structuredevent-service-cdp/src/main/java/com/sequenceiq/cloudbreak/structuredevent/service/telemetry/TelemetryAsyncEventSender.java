package com.sequenceiq.cloudbreak.structuredevent.service.telemetry;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventSenderService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class TelemetryAsyncEventSender implements CDPStructuredEventSenderService {

    public static final String TELEMETRY_EVENT_LOG_MESSAGE = "TELEMETRY_EVENT_LOG_MESSAGE";

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void create(CDPStructuredEvent structuredEvent) {
        sendAsyncEvent(TELEMETRY_EVENT_LOG_MESSAGE, eventFactory.createEvent(structuredEvent));
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }
}
