package com.sequenceiq.periscope.monitor.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.model.UpdateStackJson;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.ScalingStatus;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.service.CloudbreakService;
import com.sequenceiq.periscope.service.HistoryService;

@Component("ScalingRequest")
@Scope("prototype")
public class ScalingRequest implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingRequest.class);
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
        MDCBuilder.buildMdcContext(cluster);
        try {
            int scalingAdjustment = desiredNodeCount - totalNodes;
            if (scalingAdjustment > 0) {
                scaleUp(scalingAdjustment, totalNodes);
            } else {
                scaleDown(scalingAdjustment, totalNodes);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve an oauth token from the identity server", e);
        }
    }

    private void scaleUp(int scalingAdjustment, int totalNodes) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
        ambariAddressJson.setAmbariAddress(ambari);
        try {
            LOGGER.info("Sending request to add {} instance(s) and install services", scalingAdjustment);
            Long stackId = cloudbreakService.stackEndpoint().getStackForAmbari(ambariAddressJson).getId();
            UpdateStackJson updateStackJson = new UpdateStackJson();
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setWithClusterEvent(true);
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            instanceGroupAdjustmentJson.setInstanceGroup(hostGroup);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            cloudbreakService.stackEndpoint().put(stackId, updateStackJson);
            historyService.createEntry(ScalingStatus.SUCCESS, "Upscale successfully triggered", totalNodes, policy);
        } catch (Exception e) {
            historyService.createEntry(ScalingStatus.FAILED, "Couldn't trigger upscaling due to: " + e.getMessage(), totalNodes, policy);
            LOGGER.error("Error adding nodes to cluster", e);
        }
    }

    private void scaleDown(int scalingAdjustment, int totalNodes) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
        ambariAddressJson.setAmbariAddress(ambari);
        try {
            LOGGER.info("Sending request to remove {} node(s) from host group '{}'", scalingAdjustment, hostGroup);
            Long stackId = cloudbreakService.stackEndpoint().getStackForAmbari(ambariAddressJson).getId();
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            hostGroupAdjustmentJson.setWithStackUpdate(true);
            hostGroupAdjustmentJson.setHostGroup(hostGroup);
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            cloudbreakService.clusterEndpoint().put(stackId, updateClusterJson);
            historyService.createEntry(ScalingStatus.SUCCESS, "Downscale successfully triggered", totalNodes, policy);
        } catch (Exception e) {
            historyService.createEntry(ScalingStatus.FAILED, "Couldn't trigger downscaling due to: " + e.getMessage(), totalNodes, policy);
            LOGGER.error("Error removing nodes from the cluster", e);
        }
    }

}
