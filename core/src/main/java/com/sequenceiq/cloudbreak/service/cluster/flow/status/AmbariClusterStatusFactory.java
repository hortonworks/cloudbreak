package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariAdapter;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariAdapter.ClusterStatusResult;

@Component
public class AmbariClusterStatusFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterStatusFactory.class);

    @Inject
    private AmbariAdapter ambariAdapter;

    public ClusterStatus createClusterStatus(AmbariClient ambariClient, String blueprint) {
        ClusterStatus clusterStatus;
        if (!isAmbariServerRunning(ambariClient)) {
            clusterStatus = ClusterStatus.AMBARISERVER_NOT_RUNNING;
        } else if (blueprint != null) {
            clusterStatus = determineClusterStatus(ambariClient);
        } else {
            clusterStatus = ClusterStatus.AMBARISERVER_RUNNING;
        }
        return clusterStatus;
    }

    private boolean isAmbariServerRunning(AmbariClient ambariClient) {
        boolean result;
        try {
            result = "RUNNING".equals(ambariClient.healthCheck());
        } catch (Exception ignored) {
            result = false;
        }
        return result;
    }

    private ClusterStatus determineClusterStatus(AmbariClient ambariClient) {
        ClusterStatus clusterStatus;
        try {
            Map<String, List<Integer>> ambariOperations = ambariClient.getRequests("IN_PROGRESS", "PENDING");
            if (!ambariOperations.isEmpty()) {
                clusterStatus = ClusterStatus.PENDING;
            } else {
                ClusterStatusResult statusResult = ambariAdapter.getClusterStatusHostComponentMap(ambariClient);

                clusterStatus = statusResult.getClusterStatus();
                clusterStatus.setStatusReasonArg(statusResult.getComponentsInStatus());
            }
        } catch (Exception ex) {
            LOGGER.warn("An error occurred while trying to reach Ambari.", ex);
            clusterStatus = ClusterStatus.UNKNOWN;
        }
        return clusterStatus;
    }
}
