package com.sequenceiq.cloudbreak.core;

import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.PollingResult;

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
