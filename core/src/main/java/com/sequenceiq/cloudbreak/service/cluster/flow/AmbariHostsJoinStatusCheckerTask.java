package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask;

@Component
public class AmbariHostsJoinStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariHostsCheckerContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariHostsJoinStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AmbariHostsCheckerContext ambariHostsCheckerContext) {
        try {
            return areAllHostsJoined(ambariHostsCheckerContext);
        } catch (Exception e) {
            LOGGER.info("Not all hosts joined yet. Continuing polling.", e);
            return false;
        }
    }

    private boolean areAllHostsJoined(AmbariHostsCheckerContext ambariHostsCheckerContext) {
        AmbariClient ambariClient = ambariHostsCheckerContext.getAmbariClient();
        Map<String, String> hostNamesToStatuses = ambariClient.getHostStatuses();
        boolean allHostsJoined = true;
        for (InstanceMetaData host : ambariHostsCheckerContext.getHostsInCluster()) {
            if (!isHostJoined(host, hostNamesToStatuses)) {
                LOGGER.info("Host {} is currently not part of the cluster, waiting for it to join.", host.getDiscoveryFQDN());
                allHostsJoined = false;
            }
        }
        return allHostsJoined;
    }

    private boolean isHostJoined(InstanceMetaData hostMetadata, Map<String, String> hostNamesToStatuses) {
        for (Entry<String, String> hostWithState : hostNamesToStatuses.entrySet()) {
            if (hostWithState.getKey().equals(hostMetadata.getDiscoveryFQDN()) && !"UNKNOWN".equals(hostWithState.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handleTimeout(AmbariHostsCheckerContext t) {
        LOGGER.error("Operation timed out. Failed to find all '{}' Ambari hosts. Stack: '{}'", t.getHostCount(), t.getStack().getId());
    }

    @Override
    public String successMessage(AmbariHostsCheckerContext t) {
        return String.format("Ambari client found all %s hosts for stack '%s'", t.getHostCount(), t.getStack().getId());
    }

}
