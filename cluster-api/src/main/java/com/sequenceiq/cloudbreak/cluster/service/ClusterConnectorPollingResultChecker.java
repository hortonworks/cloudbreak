package com.sequenceiq.cloudbreak.cluster.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.polling.PollingResult;

@Service
public class ClusterConnectorPollingResultChecker {

    public void checkPollingResult(PollingResult pollingResult, String message) throws ClusterException {
        if (pollingResult.isExited()) {
            throw new CancellationException("Stack or cluster in delete in progress phase.");
        } else if (pollingResult.isTimeout() || pollingResult.isFailure()) {
            throw new ClusterException(message, pollingResult);
        }
    }
}
