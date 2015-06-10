package com.sequenceiq.cloudbreak.cloud.handler;

import com.sequenceiq.cloudbreak.cloud.notification.PollingNotifier;
import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;

import reactor.fn.Consumer;

public class PollingHandlerFactory {
    private PollingHandlerFactory() {
    }

    public static Consumer<Long> createStartPollingHandler(PollingInfo pollingInfo, PollingNotifier pollingNotifier) {
        return new SchedulablePollingHandler(pollingInfo, pollingNotifier);
    }

}
