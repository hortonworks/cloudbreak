package com.sequenceiq.cloudbreak.cloud.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.notification.model.DefaultPollingNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.PollingResultNotification;
import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;

public class PollingNotificationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingNotificationFactory.class);

    private PollingNotificationFactory() {
    }

    public static DefaultPollingNotification createPollingNotification(PollingInfo pollingInfo) {
        LOGGER.debug("Creating polling notification with: {}", pollingInfo);
        return new DefaultPollingNotification(pollingInfo);
    }

    public static PollingResultNotification createPollingResultNotification(PollingInfo pollingInfo) {
        LOGGER.debug("Creating polling result notification with: {}", pollingInfo);
        return new PollingResultNotification(pollingInfo);

    }
}
