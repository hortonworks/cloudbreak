package com.sequenceiq.cloudbreak.cloud.configuration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceRetrievalNotification;

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

    @Inject
    private Consumer<Event<ResourceRetrievalNotification>> resourceRetrievalHandler;

    @PostConstruct
    public void initialize() {
        eventBus.on(Selectors.$("resource-persisted"), resourcePersistenceHandler);
        eventBus.on(Selectors.$("resource-retrieved"), resourceRetrievalHandler);
    }

}
