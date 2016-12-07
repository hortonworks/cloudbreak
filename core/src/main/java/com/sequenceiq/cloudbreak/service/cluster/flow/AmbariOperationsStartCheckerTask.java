package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationsStatusCheckerTask.FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationsStatusCheckerTask.PENDING;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;

@Component
public class AmbariOperationsStartCheckerTask extends ClusterBasedStatusCheckerTask<AmbariOperations> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariOperationsStartCheckerTask.class);

    private static final int MAX_RETRY = 3;

    @Override
    public boolean checkStatus(AmbariOperations t) {
        Map<String, Integer> installRequests = t.getRequests();
        boolean allInProgress = true;
        for (Entry<String, Integer> request : installRequests.entrySet()) {
            AmbariClient ambariClient = t.getAmbariClient();
            BigDecimal installProgress = Optional.ofNullable(ambariClient.getRequestProgress(request.getValue())).orElse(PENDING);
            LOGGER.info("Ambari operation start: '{}', Progress: {}", request.getKey(), installProgress);
            if (FAILED.compareTo(installProgress) == 0) {
                boolean failed = true;
                for (int i = 0; i < MAX_RETRY; i++) {
                    if (ambariClient.getRequestProgress(request.getValue()).compareTo(FAILED) != 0) {
                        failed = false;
                        break;
                    }
                }
                if (failed) {
                    throw new AmbariOperationFailedException(String.format("Ambari operation start failed: [component:'%s', requestID: '%s']", request.getKey(),
                            request.getValue()));
                }
            }
            allInProgress = allInProgress && PENDING.compareTo(installProgress) != 0;
        }
        return allInProgress;
    }

    @Override
    public void handleTimeout(AmbariOperations t) {
        throw new IllegalStateException(String.format("Ambari operations start timed out: %s", t.getRequests()));
    }

    @Override
    public String successMessage(AmbariOperations t) {
        return String.format("Requested Ambari operations started: %s", t.getRequests().toString());
    }

    @Override
    public void handleException(Exception e) {
        LOGGER.error("Ambari operation start failed.", e);
    }

}