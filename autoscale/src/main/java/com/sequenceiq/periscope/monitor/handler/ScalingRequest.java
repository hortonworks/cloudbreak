package com.sequenceiq.periscope.monitor.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.HistoryService;

@Component("ScalingRequest")
@Scope("prototype")
public class ScalingRequest implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingRequest.class);

    private final int desiredNodeCount;

    private final int totalNodes;

    private final Cluster cluster;

    private final ScalingPolicy policy;

    @Inject
    private CloudbreakClient cloudbreakClient;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

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
        History history = null;
        try {
            LOGGER.info("Sending request to add {} instance(s) and install services", scalingAdjustment);
            Long stackId = cloudbreakClient.stackEndpoint().getStackForAmbari(ambariAddressJson).getId();
            UpdateStackJson updateStackJson = new UpdateStackJson();
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setWithClusterEvent(true);
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            instanceGroupAdjustmentJson.setInstanceGroup(hostGroup);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            cloudbreakClient.stackEndpoint().put(stackId, updateStackJson);
            history = historyService.createEntry(ScalingStatus.SUCCESS, "Upscale successfully triggered", totalNodes, policy);
        } catch (Exception e) {
            history = historyService.createEntry(ScalingStatus.FAILED, "Couldn't trigger upscaling due to: " + e.getMessage(), totalNodes, policy);
            LOGGER.error("Error adding nodes to cluster", e);
        } finally {
            notificationSender.send(history);
        }
    }

    private void scaleDown(int scalingAdjustment, int totalNodes) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
        ambariAddressJson.setAmbariAddress(ambari);
        History history = null;
        try {
            LOGGER.info("Sending request to remove {} node(s) from host group '{}'", scalingAdjustment, hostGroup);
            Long stackId = cloudbreakClient.stackEndpoint().getStackForAmbari(ambariAddressJson).getId();
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            hostGroupAdjustmentJson.setWithStackUpdate(true);
            hostGroupAdjustmentJson.setHostGroup(hostGroup);
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            cloudbreakClient.clusterEndpoint().put(stackId, updateClusterJson);
            history = historyService.createEntry(ScalingStatus.SUCCESS, "Downscale successfully triggered", totalNodes, policy);
        } catch (Exception e) {
            history = historyService.createEntry(ScalingStatus.FAILED, "Couldn't trigger downscaling due to: " + e.getMessage(), totalNodes, policy);
            LOGGER.error("Error removing nodes from the cluster", e);
        } finally {
            notificationSender.send(history);
        }
    }

}
