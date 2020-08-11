package com.sequenceiq.cloudbreak.structuredevent.kafka;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.StructuredEventSenderService;
import com.sequenceiq.cloudbreak.structuredevent.conf.StructuredEventEnablementConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class LegacyAsyncKafkaStructuredEventSender implements StructuredEventSenderService {

    public static final String KAFKA_EVENT_LOG_MESSAGE = "KAFKA_EVENT_LOG_MESSAGE";

    @Inject
    private StructuredEventEnablementConfig structuredEventEnablementConfig;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Override
    public boolean isEnabled() {
        return structuredEventEnablementConfig.isKafkaConfigured();
    }

    @Override
    public void create(StructuredEvent structuredEvent) {
        sendAsyncEvent(KAFKA_EVENT_LOG_MESSAGE, eventFactory.createEvent(structuredEvent));
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }
}
