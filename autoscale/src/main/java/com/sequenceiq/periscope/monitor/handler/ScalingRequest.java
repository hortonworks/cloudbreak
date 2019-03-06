package com.sequenceiq.periscope.monitor.handler;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.AmbariAddressV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakIdentityClient;
import com.sequenceiq.cloudbreak.common.type.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.MetricType;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.PeriscopeMetricService;

@Component("ScalingRequest")
@Scope("prototype")
public class ScalingRequest implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingRequest.class);

    private static final int STATUSREASON_MAX_LENGTH = 255;

    private final int desiredNodeCount;

    private final int totalNodes;

    private final Cluster cluster;

    private final ScalingPolicy policy;

    @Inject
    private CloudbreakIdentityClient cloudbreakClient;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Inject
    private PeriscopeMetricService metricService;

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
            LOGGER.info("Error while executing ScaleRequest", e);
        }
    }

    private void scaleUp(int scalingAdjustment, int totalNodes) {
        metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_TRIGGERED);
        if (scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(scalingAdjustment)) {
            LOGGER.debug("Upscale requested for {} nodes. Upscaling with the maximum allowed of {} node(s)",
                    scalingAdjustment, scalingHardLimitsService.getMaxUpscaleStepInNodeCount());
            scalingAdjustment = scalingHardLimitsService.getMaxUpscaleStepInNodeCount();
        }
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        AmbariAddressV4Request ambariAddressJson = new AmbariAddressV4Request();
        ambariAddressJson.setAmbariAddress(ambari);
        String statusReason = null;
        ScalingStatus scalingStatus = null;
        try {
            LOGGER.debug("Sending request to add {} instance(s) into host group '{}', triggered policy '{}'", scalingAdjustment, hostGroup, policy.getName());
            Long stackId = cloudbreakClient.autoscaleEndpoint().getStackForAmbari(ambariAddressJson).getId();
            UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
            updateStackJson.setWithClusterEvent(true);
            InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            instanceGroupAdjustmentJson.setInstanceGroup(hostGroup);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            cloudbreakClient.autoscaleEndpoint().putStack(stackId, cluster.getClusterPertain().getUserId(), updateStackJson);
            scalingStatus = ScalingStatus.SUCCESS;
            statusReason = "Upscale successfully triggered";
            metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_SUCCESSFUL);
        } catch (RuntimeException e) {
            scalingStatus = ScalingStatus.FAILED;
            statusReason = "Couldn't trigger upscaling due to: " + e.getMessage();
            LOGGER.info(statusReason, e);
            metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_FAILED);
        } finally {
            createHistoryAndNotify(totalNodes, statusReason, scalingStatus);
        }
    }

    private void scaleDown(int scalingAdjustment, int totalNodes) {
        metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_TRIGGERED);
        String hostGroup = policy.getHostGroup();
        String ambari = cluster.getHost();
        AmbariAddressV4Request ambariAddressJson = new AmbariAddressV4Request();
        ambariAddressJson.setAmbariAddress(ambari);
        String statusReason = null;
        ScalingStatus scalingStatus = null;
        try {
            LOGGER.debug("Sending request to remove {} node(s) from host group '{}', triggered policy '{}'", scalingAdjustment, hostGroup, policy.getName());
            Long stackId = cloudbreakClient.autoscaleEndpoint().getStackForAmbari(ambariAddressJson).getId();
            UpdateClusterV4Request updateClusterJson = new UpdateClusterV4Request();
            HostGroupAdjustmentV4Request hostGroupAdjustmentJson = new HostGroupAdjustmentV4Request();
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            hostGroupAdjustmentJson.setWithStackUpdate(true);
            hostGroupAdjustmentJson.setHostGroup(hostGroup);
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            cloudbreakClient.autoscaleEndpoint().putCluster(stackId, cluster.getClusterPertain().getUserId(), updateClusterJson);
            scalingStatus = ScalingStatus.SUCCESS;
            statusReason = "Downscale successfully triggered";
            metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_SUCCESSFUL);
        } catch (Exception e) {
            scalingStatus = ScalingStatus.FAILED;
            metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_FAILED);
            statusReason = "Couldn't trigger downscaling due to: " + e.getMessage();
            LOGGER.info(statusReason, e);
        } finally {
            createHistoryAndNotify(totalNodes, statusReason, scalingStatus);
        }
    }

    private void createHistoryAndNotify(int totalNodes, String statusReason, ScalingStatus scalingStatus) {
        History history = historyService.createEntry(scalingStatus, StringUtils.substring(statusReason, 0, STATUSREASON_MAX_LENGTH), totalNodes, policy);
        notificationSender.send(policy.getAlert().getCluster(), history);
    }
}
