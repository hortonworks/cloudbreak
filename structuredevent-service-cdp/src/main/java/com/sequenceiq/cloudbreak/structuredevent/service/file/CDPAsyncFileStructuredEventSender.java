package com.sequenceiq.cloudbreak.structuredevent.service.file;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.conf.StructuredEventEnablementConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventSenderService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CDPAsyncFileStructuredEventSender implements CDPStructuredEventSenderService {

    public static final String SAVE_STRUCTURED_EVENT_TO_FILE = "SAVE_STRUCTURED_EVENT_TO_FILE";

    @Inject
    private StructuredEventEnablementConfig structuredEventEnablementConfig;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Override
    public boolean isEnabled() {
        return structuredEventEnablementConfig.isFilePathConfigured();
    }

    @Override
    public void create(CDPStructuredEvent structuredEvent) {
        sendAsyncEvent(SAVE_STRUCTURED_EVENT_TO_FILE, eventFactory.createEvent(structuredEvent));
    }

    private void sendAsyncEvent(String selector, Event<?> event) {
        eventBus.notify(selector, event);
    }

}
