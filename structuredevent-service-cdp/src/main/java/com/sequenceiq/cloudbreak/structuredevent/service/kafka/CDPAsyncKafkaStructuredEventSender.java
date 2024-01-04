package com.sequenceiq.cloudbreak.structuredevent.service.kafka;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.structuredevent.conf.StructuredEventEnablementConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventSenderService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@Component
public class CDPAsyncKafkaStructuredEventSender implements CDPStructuredEventSenderService {

    public static final String KAFKA_EVENT_LOG_MESSAGE = "CDP_KAFKA_EVENT_LOG_MESSAGE";

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
    public void create(CDPStructuredEvent structuredEvent) {
        sendAsyncEvent(KAFKA_EVENT_LOG_MESSAGE, eventFactory.createEvent(structuredEvent));
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }
}
