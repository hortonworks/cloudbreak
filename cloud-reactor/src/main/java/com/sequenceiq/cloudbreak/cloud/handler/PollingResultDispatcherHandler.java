package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.PollingNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.PollingResultNotification;

import reactor.bus.Event;

@Component
public class PollingResultDispatcherHandler implements CloudPlatformEventHandler<PollingResultNotification> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingResultDispatcherHandler.class);

    @Inject
    private PollingNotifier pollingNotifier;

    @Override
    public void accept(Event<PollingResultNotification> dummyPollingNotificationEvent) {
        LOGGER.info("Polling result notification received: {}", dummyPollingNotificationEvent);
        PollingResultNotification notification = dummyPollingNotificationEvent.getData();

        LOGGER.debug("Dispatch polling result: {}", notification.pollingInfo());
        switch (notification.pollingInfo().pollingStatus()) {
            case TERMINATED:
                LOGGER.debug("Polling is terminated.");
                throw new IllegalStateException("Unimplemented case!");
                // TODO don't forget the break;!!!
            case ACTIVE:
                LOGGER.debug("Polling is active.");
                pollingNotifier.scheduleNewPollingCycle(notification.pollingInfo());
                break;
            case FAILED:
                //TODO
                LOGGER.debug("Polling is failed.");
                throw new IllegalStateException("Unimplemented case!");
                // TODO don't forget the break;!!!
            case SUCCESS:
                LOGGER.debug("Polling success");
                throw new IllegalStateException("Unimplemented case!");
                // TODO don't forget the break;!!!
            default:
                throw new IllegalStateException("Polling in unsupported state!");
        }
    }

    @Override
    public Class<PollingResultNotification> type() {
        return PollingResultNotification.class;
    }
}
