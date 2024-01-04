package com.sequenceiq.cloudbreak.structuredevent.service.telemetry;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventSenderService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@Component
public class LegacyTelemetryAsyncEventSender implements StructuredEventSenderService {

    public static final String LEGACY_TELEMETRY_EVENT_LOG_MESSAGE = "LEGACY_TELEMETRY_EVENT_LOG_MESSAGE";

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void create(StructuredEvent structuredEvent) {
        sendAsyncEvent(LEGACY_TELEMETRY_EVENT_LOG_MESSAGE, eventFactory.createEvent(structuredEvent));
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }
}
