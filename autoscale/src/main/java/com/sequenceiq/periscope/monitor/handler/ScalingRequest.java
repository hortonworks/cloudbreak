package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_ACTIVITY_SUCCESS;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.common.MessageCode;
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

    private List<String> decommissionNodeIds;

    @Inject
    private CloudbreakInternalCrnClient cloudbreakCrnClient;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Inject
    private PeriscopeMetricService metricService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private CloudbreakMessagesService messagesService;

    public ScalingRequest(Cluster cluster, ScalingPolicy policy, int totalNodes, int desiredNodeCount, List<String> decommissionNodeIds) {
        this.cluster = cluster;
        this.policy = policy;
        this.totalNodes = totalNodes;
        this.desiredNodeCount = desiredNodeCount;
        this.decommissionNodeIds = decommissionNodeIds;
    }

    @Override
    public void run() {
        MDCBuilder.buildMdcContext(cluster);
        try {
            int scalingAdjustment = desiredNodeCount - totalNodes;
            if (!decommissionNodeIds.isEmpty()) {
                scaleDownByNodeIds(decommissionNodeIds);
            } else if (scalingAdjustment > 0) {
                scaleUp(scalingAdjustment, totalNodes);
            } else if (scalingAdjustment < 0) {
                scaleDown(scalingAdjustment, totalNodes);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Error while executing ScaleRequest", e);
        }
    }

    private void scaleUp(int scalingAdjustment, int totalNodes) {
        metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_TRIGGERED);
        if (scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(scalingAdjustment)) {
            LOGGER.debug("Upscale requested for '{}' nodes. Upscaling with the maximum allowed of '{}' node(s)",
                    scalingAdjustment, scalingHardLimitsService.getMaxUpscaleStepInNodeCount());
            scalingAdjustment = scalingHardLimitsService.getMaxUpscaleStepInNodeCount();
        }
        String hostGroup = policy.getHostGroup();
        String statusReason = null;
        ScalingStatus scalingStatus = null;
        String stackCrn = cluster.getStackCrn();
        String userCrn = cluster.getClusterPertain().getUserCrn();
        try {

            LOGGER.info("Sending request to add '{}' instance(s) into host group '{}', triggered adjustmentType '{}', cluster '{}', user '{}'",
                    scalingAdjustment, hostGroup, policy.getAdjustmentType(), stackCrn, userCrn);
            UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
            updateStackJson.setWithClusterEvent(true);
            InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            instanceGroupAdjustmentJson.setInstanceGroup(hostGroup);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);

            cloudbreakCrnClient.withUserCrn(userCrn).autoscaleEndpoint().putStack(stackCrn, cluster.getClusterPertain().getUserId(), updateStackJson);
            scalingStatus = ScalingStatus.SUCCESS;
            statusReason =  getMessageForCBStatus(ScalingStatus.SUCCESS.name());
            metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_SUCCESSFUL);
        } catch (RuntimeException e) {
            scalingStatus = ScalingStatus.FAILED;
            statusReason = getMessageForCBStatus(e.getMessage());
            LOGGER.error("Couldn't trigger upscaling for host group '{}', cluster '{}', desiredNodeCount '{}', error '{}' ",
                    hostGroup, cluster.getStackCrn(), desiredNodeCount, e);
            metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_FAILED);
        } finally {
            createHistoryAndNotify(scalingAdjustment, totalNodes, statusReason, scalingStatus);
        }
    }

    private void scaleDown(int scalingAdjustment, int totalNodes) {
        metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_TRIGGERED);
        String hostGroup = policy.getHostGroup();
        String statusReason = null;
        ScalingStatus scalingStatus = null;
        String stackCrn = cluster.getStackCrn();
        String userCrn = cluster.getClusterPertain().getUserCrn();
        try {
            LOGGER.info("Sending request to remove '{}' node(s) from host group '{}', triggered adjustmentType '{}', cluster '{}', user '{}'",
                    scalingAdjustment, hostGroup, policy.getAdjustmentType(), stackCrn, userCrn);
            UpdateClusterV4Request updateClusterJson = new UpdateClusterV4Request();
            HostGroupAdjustmentV4Request hostGroupAdjustmentJson = new HostGroupAdjustmentV4Request();
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            hostGroupAdjustmentJson.setWithStackUpdate(true);
            hostGroupAdjustmentJson.setHostGroup(hostGroup);
            hostGroupAdjustmentJson.setValidateNodeCount(false);
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            cloudbreakCrnClient.withUserCrn(userCrn).autoscaleEndpoint()
                    .putCluster(stackCrn, cluster.getClusterPertain().getUserId(), updateClusterJson);
            scalingStatus = ScalingStatus.SUCCESS;
            statusReason =  getMessageForCBStatus(ScalingStatus.SUCCESS.name());
            metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_SUCCESSFUL);
        } catch (Exception e) {
            scalingStatus = ScalingStatus.FAILED;
            metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_FAILED);
            statusReason = getMessageForCBStatus(e.getMessage());
            LOGGER.error("Couldn't trigger downscaling for host group '{}', cluster '{}', desiredNodeCount '{}', error '{}' ",
                    hostGroup, cluster.getStackCrn(), desiredNodeCount, e);
        } finally {
            createHistoryAndNotify(scalingAdjustment, totalNodes, statusReason, scalingStatus);
        }
    }

    private void scaleDownByNodeIds(List<String> decommissionNodeIds) {
        metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_TRIGGERED);
        String hostGroup = policy.getHostGroup();
        String statusReason = null;
        ScalingStatus scalingStatus = null;
        try {
            LOGGER.info("Sending request to remove  nodeIdCount '{}', nodeId(s) '{}' from host group '{}', cluster '{}', user '{}'",
                    decommissionNodeIds.size(), decommissionNodeIds, hostGroup, cluster.getStackCrn(),
                    cluster.getClusterPertain().getUserCrn());
            cloudbreakCommunicator.decommissionInstancesForCluster(cluster, decommissionNodeIds);
            scalingStatus = ScalingStatus.SUCCESS;
            statusReason = getMessageForCBStatus(ScalingStatus.SUCCESS.name());
            metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_SUCCESSFUL);
        } catch (Exception e) {
            scalingStatus = ScalingStatus.FAILED;
            metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_FAILED);
            statusReason = getMessageForCBStatus(e.getMessage());
            LOGGER.error("Couldn't trigger decommissioning for host group '{}', cluster '{}', decommissionNodeCount '{}', " +
                    "decommissionNodeIds '{}' error '{}' ", hostGroup, cluster.getStackCrn(), decommissionNodeIds.size(), decommissionNodeIds, e);
        } finally {
            createHistoryAndNotify(-decommissionNodeIds.size(), totalNodes, statusReason, scalingStatus);
        }
    }

    private void createHistoryAndNotify(int adjustmentCount, int totalNodes, String statusReason, ScalingStatus scalingStatus) {
        History history = historyService.createEntry(scalingStatus,
                StringUtils.left(statusReason, STATUSREASON_MAX_LENGTH), totalNodes, adjustmentCount, policy);
        notificationSender.send(policy.getAlert().getCluster(), history);
    }

    private String getMessageForCBStatus(String cbMessage) {
        switch (cbMessage) {
            case "HTTP 409 Conflict":
                return messagesService.getMessage(MessageCode.CLUSTER_UPDATE_IN_PROGRESS, List.of(policy.getAlert().getAlertType()));
            case "HTTP 400 Bad Request":
            case "HTTP 500 Internal Server Error":
            case "HTTP 404 Not Found":
                return messagesService.getMessage(MessageCode.CLUSTER_NOT_AVAILABLE, List.of(policy.getAlert().getAlertType()));
            case "SUCCESS":
                return messagesService.getMessage(AUTOSCALING_ACTIVITY_SUCCESS,
                        List.of(policy.getAlert().getAlertType(), policy.getHostGroup(), totalNodes, desiredNodeCount));
            default:
                return cbMessage;
        }
    }
}
