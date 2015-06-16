package com.sequenceiq.cloudbreak.reactor.config;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.handler.PollingInfoPersistenceHandler;
import com.sequenceiq.cloudbreak.cloud.handler.PollingNotificationHandler;
import com.sequenceiq.cloudbreak.cloud.handler.PollingResultDispatcherHandler;
import com.sequenceiq.cloudbreak.cloud.handler.ResourcePersistenceHandler;

import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

@Component
public class CloudReactorInitializer {

    @Inject
    private EventBus eventBus;

    @Inject
    private PollingNotificationHandler pollingNotificationHandler;

    @Inject
    private ResourcePersistenceHandler resourcePersistenceHandler;

    @Inject
    private PollingInfoPersistenceHandler pollingInfoPersistenceHandler;

    @Inject
    private PollingResultDispatcherHandler pollingResultDispatcherHandler;

    @PostConstruct
    public void initialize() {
        eventBus.on(Selectors.$("polling-notification"), pollingInfoPersistenceHandler);
        eventBus.on(Selectors.$("polling-info-ready"), pollingNotificationHandler);
        eventBus.on(Selectors.$("resource-allocation-persisted"), resourcePersistenceHandler);
        eventBus.on(Selectors.$("polling-cycle-done"), pollingResultDispatcherHandler);
    }

}
