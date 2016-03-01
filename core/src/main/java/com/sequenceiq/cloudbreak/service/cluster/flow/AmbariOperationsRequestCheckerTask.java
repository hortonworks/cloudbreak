package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AmbariOperationsRequestCheckerTask extends ClusterBasedStatusCheckerTask<AmbariOperations> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariOperationsRequestCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariOperations operations) {
        String requestContext = operations.getRequestContext();
        String requestStatus = operations.getRequestStatus();
        int id = operations.getAmbariClient().getRequestIdWithContext(requestContext, requestStatus);
        return id != -1;
    }

    @Override
    public void handleTimeout(AmbariOperations t) {
        throw new IllegalStateException(String.format("Ambari request operation timed out: %s", t.getRequestContext()));
    }

    @Override
    public String successMessage(AmbariOperations t) {
        return String.format("Ambari request operation started: %s", t.getRequestContext());
    }

    @Override
    public void handleException(Exception e) {
        LOGGER.error("Ambari request operation failed", e);
    }

}
