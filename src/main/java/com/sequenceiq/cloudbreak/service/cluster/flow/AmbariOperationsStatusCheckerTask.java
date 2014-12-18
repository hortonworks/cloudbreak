package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StackDependentStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;

@Component
public class AmbariOperationsStatusCheckerTask extends StackDependentStatusCheckerTask<AmbariOperationsPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariOperationsStatusCheckerTask.class);

    private static final BigDecimal COMPLETED = new BigDecimal(100.0);
    private static final BigDecimal FAILED = new BigDecimal(-1.0);
    private static final int MAX_RETRY = 3;

    @Override
    public boolean checkStatus(AmbariOperationsPollerObject t) {
        MDCBuilder.buildMdcContext(t.getStack());
        Map<String, Integer> installRequests = t.getRequests();
        boolean allFinished = true;
        for (Entry<String, Integer> request : installRequests.entrySet()) {
            AmbariClient ambariClient = t.getAmbariClient();
            BigDecimal installProgress = ambariClient.getRequestProgress(request.getValue());
            LOGGER.info("Ambari operation: '{}', Progress: {}", request.getKey(), installProgress);
            allFinished = allFinished && installProgress.compareTo(COMPLETED) == 0;
            if (installProgress.compareTo(FAILED) == 0) {
                boolean failed = true;
                for (int i = 0; i < MAX_RETRY; i++) {
                    if (ambariClient.getRequestProgress(request.getValue()).compareTo(FAILED) != 0) {
                        failed = false;
                        break;
                    }
                }
                if (failed) {
                    throw new AmbariOperationFailedException(String.format("Ambari operation failed: [component: '%s', requestID: '%s']", request.getKey(),
                            request.getValue()));
                }
            }
        }
        return allFinished;
    }

    @Override
    public void handleTimeout(AmbariOperationsPollerObject t) {
        throw new IllegalStateException(String.format("Ambari operations timed out: %s", t.getRequests()));

    }

    @Override
    public String successMessage(AmbariOperationsPollerObject t) {
        return String.format("Requested Ambari operations completed: %s", t.getRequests().toString());
    }

    @Override
    public void handleExit(AmbariOperationsPollerObject ambariOperationsPollerObject) {
        return;
    }

}
