package com.sequenceiq.cloudbreak.structuredevent.file;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.conf.StructuredEventSenderConfig;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventSenderService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AsyncFileStructuredEventSender implements StructuredEventSenderService {

    public static final String SAVE_STRUCTURED_EVENT_TO_FILE = "SAVE_STRUCTURED_EVENT_TO_FILE";

    @Inject
    private StructuredEventSenderConfig structuredEventSenderConfig;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Override
    public boolean isEnabled() {
        return structuredEventSenderConfig.isFilePathConfigured();
    }

    @Override
    public void create(StructuredEvent structuredEvent) {
        sendAsyncEvent(SAVE_STRUCTURED_EVENT_TO_FILE, eventFactory.createEvent(structuredEvent));
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }

}
