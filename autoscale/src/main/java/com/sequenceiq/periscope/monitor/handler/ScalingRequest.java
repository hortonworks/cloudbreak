package com.sequenceiq.periscope.monitor.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.common.type.ScalingHardLimitsService;
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

    @Autowired
    private ScalingHardLimitsService scalingHardLimitsService;

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
        } catch (RuntimeException e) {
            LOGGER.error("Cannot retrieve an oauth token from the identity server", e);
        }
    }

    private void scaleUp(int scalingAdjustment, int totalNodes) {
        if (scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(scalingAdjustment)) {
            LOGGER.info("Upscale requested for {} nodes. Upscaling with the maximum allowed of {} node(s)",
                    scalingAdjustment, scalingHardLimitsService.getMaxUpscaleStepInNodeCount());
            scalingAdjustment = scalingHardLimitsService.getMaxUpscaleStepInNodeCount();
        }
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
        ambariAddressJson.setAmbariAddress(ambari);
        History history = null;
        try {
            LOGGER.info("Sending request to add {} instance(s) into host group '{}', triggered policy '{}'", scalingAdjustment, hostGroup, policy.getName());
            Long stackId = cloudbreakClient.stackV1Endpoint().getStackForAmbari(ambariAddressJson).getId();
            UpdateStackJson updateStackJson = new UpdateStackJson();
            updateStackJson.setWithClusterEvent(true);
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            instanceGroupAdjustmentJson.setInstanceGroup(hostGroup);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            cloudbreakClient.stackV1Endpoint().put(stackId, updateStackJson);
            history = historyService.createEntry(ScalingStatus.SUCCESS, "Upscale successfully triggered", totalNodes, policy);
        } catch (RuntimeException e) {
            String statusReason = "Couldn't trigger upscaling due to: " + e.getMessage();
            history = historyService.createEntry(ScalingStatus.FAILED, statusReason, totalNodes, policy);
            LOGGER.error(statusReason, e);
        } finally {
            if (history != null) {
                notificationSender.send(history);
            }
        }
    }

    private void scaleDown(int scalingAdjustment, int totalNodes) {
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
        ambariAddressJson.setAmbariAddress(ambari);
        History history = null;
        try {
            LOGGER.info("Sending request to remove {} node(s) from host group '{}', triggered policy '{}'", scalingAdjustment, hostGroup, policy.getName());
            Long stackId = cloudbreakClient.stackV1Endpoint().getStackForAmbari(ambariAddressJson).getId();
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            hostGroupAdjustmentJson.setWithStackUpdate(true);
            hostGroupAdjustmentJson.setHostGroup(hostGroup);
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            cloudbreakClient.clusterEndpoint().put(stackId, updateClusterJson);
            history = historyService.createEntry(ScalingStatus.SUCCESS, "Downscale successfully triggered", totalNodes, policy);
        } catch (Exception e) {
            String statusReason = "Couldn't trigger downscaling due to: " + e.getMessage();
            history = historyService.createEntry(ScalingStatus.FAILED, statusReason, totalNodes, policy);
            LOGGER.error(statusReason, e);
        } finally {
            if (history != null) {
                notificationSender.send(history);
            }
        }
    }

}
