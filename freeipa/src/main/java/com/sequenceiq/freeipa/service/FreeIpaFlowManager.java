package com.sequenceiq.freeipa.service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Acceptable;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.reactor.api.event.BaseFlowEvent;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class FreeIpaFlowManager {
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

        reactor.notify(selector, event);
        try {
            Boolean accepted = true;
            if (event.getData().accepted() != null) {
                accepted = event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            }
            if (accepted == null || !accepted) {
                throw new RuntimeException(String.format("Stack %s has flows under operation, request not allowed."));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
