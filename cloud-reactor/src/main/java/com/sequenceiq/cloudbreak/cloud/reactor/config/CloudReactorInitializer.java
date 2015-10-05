package com.sequenceiq.cloudbreak.cloud.reactor.config;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;
import reactor.fn.Consumer;

@Component
public class CloudReactorInitializer {

    @Inject
    private EventBus eventBus;

    @Inject
    private Consumer<Event<ResourceNotification>> resourcePersistenceHandler;

    @PostConstruct
    public void initialize() {
        eventBus.on(Selectors.$("resource-persisted"), resourcePersistenceHandler);
    }

}
