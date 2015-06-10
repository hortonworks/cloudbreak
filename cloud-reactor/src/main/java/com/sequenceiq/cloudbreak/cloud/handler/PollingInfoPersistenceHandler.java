package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.PollingNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.DefaultPollingNotification;
import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;
import com.sequenceiq.cloudbreak.cloud.polling.PollingService;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

import reactor.bus.Event;

@Component
public class PollingInfoPersistenceHandler implements CloudPlatformEventHandler<DefaultPollingNotification> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingInfoPersistenceHandler.class);

    @Inject
    private Persister<DefaultPollingNotification> pollingNotificationPersister;

    @Inject
    private PollingService pollingService;

    @Inject
    private PollingNotifier pollingNotifier;


    @Override
    public void accept(Event<DefaultPollingNotification> dummyPollingNotificationEvent) {
        LOGGER.info("Polling notification received: {}", dummyPollingNotificationEvent);
        DefaultPollingNotification notification = dummyPollingNotificationEvent.getData();
        if (notification.pollingInfo().pollingStatus().equals(PollingInfo.PollingStatus.NEW)) {
            LOGGER.debug("New polling data received, persisting it: {}", notification);
            notification.pollingInfo().setPollingStatus(PollingInfo.PollingStatus.ACTIVE);
            notification = pollingNotificationPersister.persist(notification);
        } else {
            LOGGER.debug("Active polling data received, retrieving it: {}", notification);
            notification = pollingNotificationPersister.retrieve(notification);
        }
        notification.operationCompleted(notification.pollingInfo());
        LOGGER.debug("Persisted polling information available: {}", notification);
        pollingNotifier.pollingInfoPersisted(notification.pollingInfo());
    }

    @Override
    public Class<DefaultPollingNotification> type() {
        return DefaultPollingNotification.class;
    }
}
