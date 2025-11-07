package com.sequenceiq.notification.domain;

public enum NotificationFormFactor {
    SUBSCRIPTION,
    DISTRIBUTION_LIST,
    SYSTEM_MANAGED_DISTRIBUTION_LIST;

    public boolean isDistributionList() {
        return this == DISTRIBUTION_LIST || this == SYSTEM_MANAGED_DISTRIBUTION_LIST;
    }
}
