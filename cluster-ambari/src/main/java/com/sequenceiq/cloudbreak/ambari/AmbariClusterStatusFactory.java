package com.sequenceiq.cloudbreak.ambari;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class AmbariClusterStatusFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterStatusFactory.class);

    @Inject
    private AmbariAdapter ambariAdapter;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    public ClusterStatusResult createClusterStatus(Stack stack, HttpClientConfig clientConfig, boolean blueprintPresent) {
        ClusterStatusResult clusterStatus;
        AmbariClient ambariClient = ambariClientFactory.getAmbariClient(stack, stack.getCluster(), clientConfig);
        if (!isAmbariServerRunning(ambariClient)) {
            clusterStatus = ClusterStatusResult.of(ClusterStatus.AMBARISERVER_NOT_RUNNING);
        } else if (blueprintPresent) {
            clusterStatus = determineClusterStatus(ambariClient);
        } else {
            clusterStatus = ClusterStatusResult.of(ClusterStatus.AMBARISERVER_RUNNING);
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

    private ClusterStatusResult determineClusterStatus(AmbariClient ambariClient) {
        ClusterStatusResult result;
        try {
            Map<String, List<Integer>> ambariOperations = ambariClient.getRequests("IN_PROGRESS", "PENDING");
            if (!ambariOperations.isEmpty()) {
                result = ClusterStatusResult.of(ClusterStatus.PENDING);
            } else {
                result = ambariAdapter.getClusterStatusHostComponentMap(ambariClient);
            }
        } catch (Exception ex) {
            LOGGER.warn("An error occurred while trying to reach Ambari.", ex);
            result = ClusterStatusResult.of(ClusterStatus.UNKNOWN);
        }
        return result;
    }
}
