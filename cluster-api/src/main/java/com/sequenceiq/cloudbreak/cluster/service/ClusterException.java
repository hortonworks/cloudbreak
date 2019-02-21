package com.sequenceiq.cloudbreak.cluster.service;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public class ClusterException extends CloudbreakException {

    private final PollingResult pollingResult;

    public ClusterException(String message, PollingResult pollingResult) {
        super(message);
        this.pollingResult = pollingResult;
    }

    public PollingResult getPollingResult() {
        return pollingResult;
    }
}
