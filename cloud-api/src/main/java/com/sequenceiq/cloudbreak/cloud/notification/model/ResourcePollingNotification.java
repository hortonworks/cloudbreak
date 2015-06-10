package com.sequenceiq.cloudbreak.cloud.notification.model;

import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;

public class ResourcePollingNotification implements PollingNotification<PollingInfo> {
    @Override
    public PollingInfo pollingInfo() {
        return null;
    }

    @Override
    public void operationCompleted(PollingInfo pollingInfo) {

    }
}
