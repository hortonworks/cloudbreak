package com.sequenceiq.cloudbreak.service.eventbus;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

@Component
public class EventBusInitializer {

    @Inject
    private EventBus eventBus;

    @Inject
    private ResourcePersistenceHandler resourcePersistenceHandler;

    @PostConstruct
    public void initialize() {
        eventBus.on(Selectors.$("ResourceAllocationPersisted"), resourcePersistenceHandler);
    }
}
