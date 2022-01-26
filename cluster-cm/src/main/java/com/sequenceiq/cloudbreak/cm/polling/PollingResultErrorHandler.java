package com.sequenceiq.cloudbreak.cm.polling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class PollingResultErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingResultErrorHandler.class);

    public void handlePollingResult(PollingResult pollingResult, String cancellationMessage, String timeoutMessage) throws CloudbreakException {
        LOGGER.info("Poller finished with state: {}", pollingResult);
        if (pollingResult.isExited()) {
            throw new CancellationException(cancellationMessage);
        } else if (pollingResult.isTimeout()) {
            throw new CloudbreakException(timeoutMessage);
        }
    }

    public void handlePollingResult(ExtendedPollingResult pollingResult, String cancellationMessage, String timeoutMessage) throws CloudbreakException {
        handlePollingResult(pollingResult.getPollingResult(), cancellationMessage, timeoutMessage);
    }
}
