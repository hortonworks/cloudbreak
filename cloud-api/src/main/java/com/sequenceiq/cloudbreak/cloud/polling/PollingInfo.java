package com.sequenceiq.cloudbreak.cloud.polling;

/**
 * Marker for specific polling notification objects.
 */
public interface PollingInfo {
    enum PollingStatus {
        NEW, ACTIVE, SUCCESS, TERMINATED, FAILED
    }

    PollingReference pollingReference();

    PollingStatus pollingStatus();

    void setPollingStatus(PollingStatus pollingStatus);

    void increasePollingCount();

    int pollingCount();
}
