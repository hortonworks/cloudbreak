package com.sequenceiq.cloudbreak.cloud.configuration;

import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceRetrievalNotification;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

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
        eventBus.on("resource-persisted", resourcePersistenceHandler);
        eventBus.on("resource-retrieved", resourceRetrievalHandler);
    }

}
