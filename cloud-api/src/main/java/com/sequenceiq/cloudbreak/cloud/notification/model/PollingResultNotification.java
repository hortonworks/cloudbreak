package com.sequenceiq.cloudbreak.cloud.notification.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;

public class PollingResultNotification implements PollingNotification<PollingInfo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingResultNotification.class);

    private PollingInfo dummyPollingInfo;

    public PollingResultNotification(PollingInfo dummyPollingInfo) {
        this.dummyPollingInfo = dummyPollingInfo;
    }

    @Override
    public PollingInfo pollingInfo() {
        return dummyPollingInfo;
    }

    @Override
    public void operationCompleted(PollingInfo pollingInfo) {
        LOGGER.debug("TODO: Polling operation completed: {}", pollingInfo);
    }
}
