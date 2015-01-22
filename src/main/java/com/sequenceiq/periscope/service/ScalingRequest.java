package com.sequenceiq.periscope.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;

@Component("ScalingRequest")
@Scope("prototype")
public class ScalingRequest implements Runnable {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ScalingRequest.class);
    private static final String AVAILABLE = "AVAILABLE";
    private static final int STACK_RETRY_COUNT = 100;
    private static final int CLUSTER_RETRY_COUNT = 3;
    private static final int STACK_AVAILABLE_ACK = 2;
    private static final int SLEEP = 20000;
    private final long sleepTime;
    private final int stackRetryCount;
    private final int clusterRetryCount;
    private final int desiredNodeCount;
    private final int totalNodes;
    private final Cluster cluster;
    private final ScalingPolicy policy;

    @Autowired
    private CloudbreakService cloudbreakService;

    public ScalingRequest(Cluster cluster, ScalingPolicy policy, int totalNodes, int desiredNodeCount) {
        this(cluster, policy, totalNodes, desiredNodeCount, SLEEP, STACK_RETRY_COUNT, CLUSTER_RETRY_COUNT);
    }

    public ScalingRequest(Cluster cluster, ScalingPolicy policy, int totalNodes,
            int desiredNodeCount, long sleepTime, int stackRetry, int clusterRetry) {
        this.cluster = cluster;
        this.policy = policy;
        this.stackRetryCount = stackRetry;
        this.sleepTime = sleepTime;
        this.totalNodes = totalNodes;
        this.clusterRetryCount = clusterRetry;
        this.desiredNodeCount = desiredNodeCount;
    }

    @Override
    public void run() {
        try {
            CloudbreakClient client = cloudbreakService.getClient();
            int scalingAdjustment = desiredNodeCount - totalNodes;
            if (scalingAdjustment > 0) {
                scaleUp(client, scalingAdjustment);
            } else {
                scaleDown(client, scalingAdjustment);
            }
        } catch (Exception e) {
            LOGGER.error(Logger.NOT_CLUSTER_RELATED, "Cannot retrieve an oauth token from the identity server", e);
        }
    }

    private void scaleUp(CloudbreakClient client, int scalingAdjustment) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        long clusterId = cluster.getId();
        try {
            LOGGER.info(clusterId, "Sending request to add {} instance(s)", scalingAdjustment);
            int stackId = client.resolveToStackId(ambari);
            client.putStack(stackId, hostGroup, scalingAdjustment);
            boolean ready = waitForReadyState(clusterId, stackId, client);
            if (ready) {
                boolean sent = sendInstallRequest(client, scalingAdjustment, hostGroup, clusterId, stackId);
                if (sent) {
                    LOGGER.info(clusterId, "Install request successfully sent");
                } else {
                    LOGGER.info(clusterId, "Could not send the install request");
                }
            } else {
                LOGGER.info(clusterId, "Instance(s) didn't start in time, skipping scaling");
                // TODO should we terminate the launched instances?
            }
        } catch (Exception e) {
            LOGGER.error(clusterId, "Error adding nodes to cluster", e);
        }
    }

    private boolean sendInstallRequest(CloudbreakClient client, int scalingAdjustment,
            String hostGroup, long clusterId, int stackId) throws InterruptedException {
        LOGGER.info(clusterId, "Sending request to install components to the host(s)");
        int retry = 0;
        while (retry < clusterRetryCount) {
            try {
                client.putCluster(stackId, hostGroup, scalingAdjustment);
                break;
            } catch (Exception e) {
                retry++;
                Thread.sleep(sleepTime);
            }
        }
        return retry != clusterRetryCount;
    }

    private void scaleDown(CloudbreakClient client, int scalingAdjustment) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        long clusterId = cluster.getId();
        try {
            LOGGER.info(clusterId, "Sending request to remove {} node(s) from host group '{}'", scalingAdjustment, hostGroup);
            int stackId = client.resolveToStackId(ambari);
            client.putCluster(stackId, hostGroup, scalingAdjustment);
            boolean ready = waitForReadyState(clusterId, stackId, client);
            if (ready) {
                LOGGER.info(clusterId, "Sending request to terminate {} instance(s)", scalingAdjustment);
                client.putStack(stackId, hostGroup, scalingAdjustment);
            } else {
                LOGGER.info(clusterId, "Instance(s) didn't stop in time, skipping scaling");
                // TODO should we force instance termination?
            }
        } catch (Exception e) {
            LOGGER.error(clusterId, "Error removing nodes from the cluster", e);
        }
    }

    private boolean waitForReadyState(long clusterId, int stackId, CloudbreakClient client) throws InterruptedException {
        int retry = 0;
        int ready = STACK_AVAILABLE_ACK;
        while (ready > 0 && retry < stackRetryCount) {
            LOGGER.info(clusterId, "Scaling: Waiting for cluster to be {}", AVAILABLE);
            String status = client.getStackStatus(stackId);
            if (AVAILABLE.equals(status)) {
                ready--;
            } else {
                retry++;
            }
            Thread.sleep(sleepTime);
        }
        return ready == 0;
    }

}
