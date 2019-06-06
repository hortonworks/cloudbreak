package com.sequenceiq.redbeams.service;

import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RedbeamsFlowManager {
    private static final long WAIT_FOR_ACCEPT = 10L;

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    private Random random = new Random();

    public void triggerHelloworld() {
        String selector = "HELLOWORLD_CHAIN_EVENT";
        notify(selector, new BaseFlowEvent(selector, random.nextLong()));
    }

    public void notify(String selector, Acceptable acceptable) {
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(acceptable);
        notify(selector, event);
    }

    public void notify(String selector, Acceptable acceptable, Map<String, Object> headers) {
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(headers, acceptable);
        notify(selector, event);
    }

    public void notify(Selectable selectable) {
        Event<Selectable> event = eventFactory.createEvent(selectable);
        reactor.notify(selectable.selector(), event);
    }

    private void notify(String selector, Event<Acceptable> event) {
        reactor.notify(selector, event);
        try {
            Boolean accepted = true;
            if (event.getData().accepted() != null) {
                accepted = event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            }
            if (accepted == null || !accepted) {
                throw new RuntimeException(String.format("Flows under operation, request not allowed."));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
