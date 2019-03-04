package com.sequenceiq.cloudbreak.ambari.flow;

import static com.sequenceiq.cloudbreak.ambari.flow.AmbariOperationsStatusCheckerTask.FAILED;
import static com.sequenceiq.cloudbreak.ambari.flow.AmbariOperationsStatusCheckerTask.PENDING;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;

@Component
public class AmbariOperationsStartCheckerTask extends ClusterBasedStatusCheckerTask<AmbariOperations> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariOperationsStartCheckerTask.class);

    private static final int MAX_RETRY = 3;

    @Override
    public boolean checkStatus(AmbariOperations t) {
        Map<String, Integer> installRequests = t.getRequests();
        for (Entry<String, Integer> request : installRequests.entrySet()) {
            AmbariClient ambariClient = t.getAmbariClient();
            BigDecimal installProgress = Optional.ofNullable(ambariClient.getRequestProgress(request.getValue())).orElse(PENDING);
            LOGGER.debug("Ambari operation start: '{}', Progress: {}", request.getKey(), installProgress);
            if (FAILED.compareTo(installProgress) == 0) {
                boolean failed = true;
                for (int i = 0; i < MAX_RETRY; i++) {
                    if (ambariClient.getRequestProgress(request.getValue()).compareTo(FAILED) != 0) {
                        failed = false;
                        break;
                    }
                }
                if (failed) {
                    Map<String, ?> requests = (Map<String, ?>) ambariClient.getRequestStatus(request.getValue()).get("Requests");
                    String context = (String) requests.get("request_context");
                    String status = (String) requests.get("request_status");
                    throw new AmbariOperationFailedException(
                            String.format("Ambari operation start failed: [component:'%s', requestID: '%s', context: '%s', status: '%s']",
                            request.getKey(), request.getValue(), context, status));
                }
            }
            if (PENDING.compareTo(installProgress) == 0) {
                return false;
            }
        }
        return true;
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
        LOGGER.info("Ambari operation start failed.", e);
    }
}