package com.sequenceiq.cloudbreak.cloud.polling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyPollingInfo implements PollingInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyPollingInfo.class);

    private PollingStatus pollingStatus = PollingStatus.NEW;
    private NumericPollingReference pollingReference;
    private int pollingCount;

    @Override
    public PollingStatus pollingStatus() {
        return pollingStatus;
    }

    @Override
    public void setPollingStatus(PollingStatus pollingStatus) {
        LOGGER.debug("Status change from {} to {}", this.pollingStatus, pollingStatus);
        this.pollingStatus = pollingStatus;
    }

    @Override
    public NumericPollingReference pollingReference() {
        return pollingReference;
    }

    public void setPollingReference(NumericPollingReference pollingReference) {
        this.pollingReference = pollingReference;
    }

    public void setPollingCount(int pollingCount) {
        this.pollingCount = pollingCount;
    }

    @Override
    public void increasePollingCount() {
        this.pollingCount++;
    }

    @Override
    public int pollingCount() {
        return this.pollingCount;
    }


    //BEGIN GENERATED CODE

    @Override
    public String toString() {
        return "DummyPollingInfo{" +
                "pollingStatus=" + pollingStatus +
                ", pollingReference=" + pollingReference +
                ", pollingCount=" + pollingCount +
                '}';
    }

    //END GENERATED CODE
}
