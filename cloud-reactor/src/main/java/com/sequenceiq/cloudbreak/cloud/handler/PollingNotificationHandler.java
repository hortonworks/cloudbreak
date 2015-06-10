package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.PollingNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.DefaultPollingNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.PollingResultNotification;
import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;
import com.sequenceiq.cloudbreak.cloud.polling.PollingService;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

import reactor.bus.Event;

@Component
public class PollingNotificationHandler implements CloudPlatformEventHandler<PollingInfo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingNotificationHandler.class);

    @Inject
    private PollingService<PollingInfo> pollingService;

    @Inject
    private Persister<DefaultPollingNotification> pollingNotificationPersister;

    @Inject
    private PollingNotifier pollingNotifier;

    @Override
    public Class<PollingInfo> type() {
        return PollingInfo.class;
    }

    @Override
    public void accept(Event<PollingInfo> pollingInfoEvent) {
        LOGGER.debug("Polling notification event received: {}", pollingInfoEvent);
        PollingInfo freshPollingInfo = pollingService.doPoll(pollingInfoEvent.getData());
        freshPollingInfo.increasePollingCount();
        //TODO maybe this persistence operation could also be done asynchronously;
        DefaultPollingNotification notification = pollingNotificationPersister.persist(PollingNotificationFactory.createPollingNotification(freshPollingInfo));
        PollingResultNotification pollingResultNotification = PollingNotificationFactory.createPollingResultNotification(notification.pollingInfo());
        pollingNotifier.pollingCycleDone(pollingResultNotification);
    }
}
