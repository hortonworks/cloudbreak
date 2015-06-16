package com.sequenceiq.cloudbreak.cloud.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.notification.PollingNotifier;
import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;

import reactor.fn.Consumer;

/**
 * Entry point for the polling mechanism.
 * Intended to be used with the Timer API.
 */
public class SchedulablePollingHandler implements Consumer<Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulablePollingHandler.class);

    private PollingInfo pollingInfo;
    private PollingNotifier pollingNotifier;

    public SchedulablePollingHandler(PollingInfo pollingInfo, PollingNotifier pollingNotifier) {
        this.pollingInfo = pollingInfo;
        this.pollingNotifier = pollingNotifier;
    }

    @Override
    public void accept(Long id) {
        LOGGER.debug("Start polling: {}", pollingInfo);
        pollingNotifier.notifyPolling(PollingNotificationFactory.createPollingNotification(pollingInfo));
    }
}
