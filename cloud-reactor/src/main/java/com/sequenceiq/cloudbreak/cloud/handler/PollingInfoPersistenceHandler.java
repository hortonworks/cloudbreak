package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.DefaultPollingNotification;
import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;
import com.sequenceiq.cloudbreak.cloud.polling.PollingService;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class PollingInfoPersistenceHandler implements CloudPlatformEventHandler<DefaultPollingNotification> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingInfoPersistenceHandler.class);

    @Inject
    private Persister<DefaultPollingNotification> pollingNotificationPersister;

    @Inject
    private PollingService pollingService;

    @Inject
    private EventBus eventBus;


    @Override
    public void accept(Event<DefaultPollingNotification> dummyPollingNotificationEvent) {
        LOGGER.info("ResourceAllocationNotification received: {}", dummyPollingNotificationEvent);
        DefaultPollingNotification notification = dummyPollingNotificationEvent.getData();
        if (notification.pollingInfo().pollingStatus().equals(PollingInfo.PollingStatus.NEW)) {
            notification.pollingInfo().setPollingStatus(PollingInfo.PollingStatus.ACTIVE);
            notification = pollingNotificationPersister.persist(notification);
        } else {
            notification = pollingNotificationPersister.retrieve(notification);
        }
        notification.operationCompleted(notification.pollingInfo());
        //TODO create proper notification, use specific notifier instead of using the eventbus directly
        eventBus.notify("polling-info-ready", Event.wrap(notification.pollingInfo()));
    }

    @Override
    public Class<DefaultPollingNotification> type() {
        return DefaultPollingNotification.class;
    }
}
