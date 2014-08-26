package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;

public class AmbariOperationsStatusCheckerTask implements StatusCheckerTask<AmbariOperations> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariOperationsStatusCheckerTask.class);

    private static final BigDecimal COMPLETED = new BigDecimal(100.0);
    private static final BigDecimal FAILED = new BigDecimal(-1.0);

    @Override
    public boolean checkStatus(AmbariOperations t) {
        Map<String, Integer> installRequests = t.getRequests();
        boolean allFinished = true;
        for (Entry<String, Integer> request : installRequests.entrySet()) {
            BigDecimal installProgress = t.getAmbariClient().getRequestProgress(request.getValue());
            LOGGER.info("Ambari operation: '{}', Progress: {} [Stack: '{}']", request.getKey(), installProgress, t.getStackId());
            allFinished = allFinished && installProgress.compareTo(COMPLETED) == 0;
            if (installProgress.compareTo(FAILED) == 0) {
                throw new AmbariOperationFailedException(String.format("Ambari operation failed: [component: '%s', requestID: '%s']", request.getKey(),
                        request.getValue()));
            }
        }
        return allFinished;
    }

    @Override
    public void handleTimeout(AmbariOperations t) {
        throw new IllegalStateException("Ambari progress cannot be timed out.");

    }

    @Override
    public String successMessage(AmbariOperations t) {
        return String.format("Requested Ambari operations completed: %s", t.getRequests().toString());
    }

}
