package com.sequenceiq.cloudbreak.ambari.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;

@Component
public class AmbariStartupListenerTask extends ClusterBasedStatusCheckerTask<AmbariStartupPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStartupListenerTask.class);

    @Override
    public boolean checkStatus(AmbariStartupPollerObject aSPO) {
        boolean ambariRunning = false;
        LOGGER.debug("Polling Ambari server's status [Ambari server address: '{}'].", aSPO.getAmbariAddress());
        for (AmbariClient ambariClient : aSPO.getAmbariClients()) {
            try {
                String ambariHealth = ambariClient.healthCheck();
                LOGGER.debug("Ambari health check returned: {} [Ambari server address: '{}']", ambariHealth, aSPO.getAmbariAddress());
                if ("RUNNING".equals(ambariHealth)) {
                    ambariRunning = true;
                    break;
                }
            } catch (Exception e) {
                LOGGER.debug("Ambari health check failed: {}", e.getMessage());
            }
        }
        return ambariRunning;
    }

    @Override
    public void handleTimeout(AmbariStartupPollerObject ambariStartupPollerObject) {
        throw new AmbariOperationFailedException("Operation timed out. Failed to check ambari startup.");
    }

    @Override
    public String successMessage(AmbariStartupPollerObject aSPO) {
        return "Ambari startup finished with success result.";
    }
}
