package com.sequenceiq.cloudbreak.cloud.reactor.config;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.handler.ResourcePersistenceHandler;

import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

@Component
public class CloudReactorInitializer {

    @Inject
    private EventBus eventBus;


    @Inject
    private ResourcePersistenceHandler resourcePersistenceHandler;

    @PostConstruct
    public void initialize() {
        eventBus.on(Selectors.$("resource-allocation-persisted"), resourcePersistenceHandler);
    }

}
