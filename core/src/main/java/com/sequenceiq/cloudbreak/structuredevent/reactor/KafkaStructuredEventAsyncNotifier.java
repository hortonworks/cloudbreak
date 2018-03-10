package com.sequenceiq.cloudbreak.structuredevent.reactor;

import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;

@Component
public class KafkaStructuredEventAsyncNotifier {

    public static final String KAFKA_EVENT_LOG_MESSAGE = "KAFKA_EVENT_LOG_MESSAGE";

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    public void sendEventLogMessage(StructuredEvent structuredEvent) {
        sendAsyncEvent(KAFKA_EVENT_LOG_MESSAGE, eventFactory.createEvent(structuredEvent));
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }
}
