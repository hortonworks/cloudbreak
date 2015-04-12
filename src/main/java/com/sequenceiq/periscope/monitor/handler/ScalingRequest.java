package com.sequenceiq.periscope.monitor.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.ScalingStatus;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.service.CloudbreakService;
import com.sequenceiq.periscope.service.HistoryService;

@Component("ScalingRequest")
@Scope("prototype")
public class ScalingRequest implements Runnable {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(ScalingRequest.class);
    private final int desiredNodeCount;
    private final int totalNodes;
    private final Cluster cluster;
    private final ScalingPolicy policy;

    @Autowired
    private CloudbreakService cloudbreakService;
    @Autowired
    private HistoryService historyService;

    public ScalingRequest(Cluster cluster, ScalingPolicy policy, int totalNodes, int desiredNodeCount) {
        this.cluster = cluster;
        this.policy = policy;
        this.totalNodes = totalNodes;
        this.desiredNodeCount = desiredNodeCount;
    }

    @Override
    public void run() {
        try {
            CloudbreakClient client = cloudbreakService.getClient();
            int scalingAdjustment = desiredNodeCount - totalNodes;
            if (scalingAdjustment > 0) {
                scaleUp(client, scalingAdjustment, totalNodes);
            } else {
                scaleDown(client, scalingAdjustment, totalNodes);
            }
        } catch (Exception e) {
            LOGGER.error(Logger.NOT_CLUSTER_RELATED, "Cannot retrieve an oauth token from the identity server", e);
        }
    }

    private void scaleUp(CloudbreakClient client, int scalingAdjustment, int totalNodes) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        long clusterId = cluster.getId();
        try {
            LOGGER.info(clusterId, "Sending request to add {} instance(s) and install services", scalingAdjustment);
            int stackId = client.resolveToStackId(ambari);
            client.putStack(stackId, hostGroup, scalingAdjustment, true);
            historyService.createEntry(ScalingStatus.SUCCESS, "Upscale successfully triggered", totalNodes, policy);
        } catch (Exception e) {
            historyService.createEntry(ScalingStatus.FAILED, "Couldn't trigger upscaling due to: " + e.getMessage(), totalNodes, policy);
            LOGGER.error(clusterId, "Error adding nodes to cluster", e);
        }
    }

    private void scaleDown(CloudbreakClient client, int scalingAdjustment, int totalNodes) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        long clusterId = cluster.getId();
        try {
            LOGGER.info(clusterId, "Sending request to remove {} node(s) from host group '{}'", scalingAdjustment, hostGroup);
            int stackId = client.resolveToStackId(ambari);
            client.putCluster(stackId, hostGroup, scalingAdjustment, true);
            historyService.createEntry(ScalingStatus.SUCCESS, "Downscale successfully triggered", totalNodes, policy);
        } catch (Exception e) {
            historyService.createEntry(ScalingStatus.FAILED, "Couldn't trigger downscaling due to: " + e.getMessage(), totalNodes, policy);
            LOGGER.error(clusterId, "Error removing nodes from the cluster", e);
        }
    }

}
