package com.sequenceiq.cloudbreak.cluster.service;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isFailure;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.polling.PollingResult;

@Service
public class ClusterConnectorPollingResultChecker {

    public void checkPollingResult(PollingResult pollingResult, String message) throws ClusterException {
        if (isExited(pollingResult)) {
            throw new CancellationException("Stack or cluster in delete in progress phase.");
        } else if (isTimeout(pollingResult) || isFailure(pollingResult)) {
            throw new ClusterException(message, pollingResult);
        }
    }
}
