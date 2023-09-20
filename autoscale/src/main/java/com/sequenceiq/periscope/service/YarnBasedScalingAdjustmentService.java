package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.ActivityStatus.METRICS_COLLECTION_SUCCESS;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_YARN_RECOMMENDATION_SUCCESS;
import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;
import static com.sequenceiq.periscope.model.ScalingAdjustmentType.STOPSTART;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.model.ScalingAdjustmentType;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.evaluator.load.YarnResponseUtils;
import com.sequenceiq.periscope.monitor.sender.ScalingEventSender;
import com.sequenceiq.periscope.utils.ClusterUtils;
import com.sequenceiq.periscope.utils.StackResponseUtils;
import com.sequenceiq.periscope.utils.TimeUtil;

@Service
public class YarnBasedScalingAdjustmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnBasedScalingAdjustmentService.class);

    private static final String UPSCALE_MESSAGE = "Proceeding with cluster upscale";

    private static final String DOWNSCALE_MESSAGE = "Proceeding with cluster downscale";

    @Inject
    private YarnResponseUtils yarnResponseUtils;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private ScalingActivityService scalingActivityService;

    @Inject
    private Clock clock;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private YarnMetricsClient yarnMetricsClient;

    @Inject
    private ClusterService clusterService;

    @Inject
    private PeriscopeMetricService metricService;

    @Inject
    private ScalingEventSender eventSender;

    public void pollYarnMetricsAndScaleCluster(Cluster cluster, String pollingUserCrn, boolean stopStartEnabled, StackV4Response stackV4Response)
            throws Exception {

        LoadAlert loadAlert = cluster.getLoadAlerts().iterator().next();
        LoadAlertConfiguration loadAlertConfiguration = loadAlert.getLoadAlertConfiguration();
        String policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();

        LoadAlertPolicyHostGroupInstanceInfo policyHostGroupInstanceInfo =
                new LoadAlertPolicyHostGroupInstanceInfo(loadAlert, policyHostGroup,
                        stackResponseUtils.getCloudInstanceIdsForHostGroup(stackV4Response, policyHostGroup),
                        stackResponseUtils.getCloudInstanceIdsWithServicesHealthyForHostGroup(stackV4Response, policyHostGroup),
                        stackResponseUtils.getStoppedCloudInstanceIdsInHostGroup(stackV4Response, policyHostGroup));

        int serviceHealthyHostGroupSize = policyHostGroupInstanceInfo.getServicesHealthyInstanceIds().size();
        int existingHostGroupSize = policyHostGroupInstanceInfo.getHostFqdnsToInstanceId().size();

        int maxResourceValueOffset = loadAlertConfiguration.getMaxResourceValue() - (stopStartEnabled ? serviceHealthyHostGroupSize : existingHostGroupSize);
        int minResourceValueOffset = serviceHealthyHostGroupSize - loadAlertConfiguration.getMinResourceValue();

        int maxAllowedUpScale = Math.max(maxResourceValueOffset, 0);
        int maxAllowedDownScale = Math.max(minResourceValueOffset, 0);

        LOGGER.info("Various counts: hostFqdnsToInstanceId={}, servicesHealthyHostFqdnsToInstanceId: {}, stoppedHostFqdnsToInstanceId: {}, " +
                        "maxResourceValueOffset={}, minResourceValueOffset={}, maxAllowedUpScale={}, maxAllowedDownScale={}",
                existingHostGroupSize, serviceHealthyHostGroupSize, policyHostGroupInstanceInfo.getStoppedHostInstanceIds().size(),
                maxResourceValueOffset, minResourceValueOffset, maxAllowedUpScale, maxAllowedDownScale);

        Optional<Integer> maxDecommissionNodeCount = Optional.of(maxResourceValueOffset)
                .filter(mandatoryDownscale -> mandatoryDownscale < 0).map(downscale -> -1 * downscale);

        long start = clock.getCurrentTimeMillis();
        YarnScalingServiceV1Response yarnResponse =
                yarnMetricsClient.getYarnMetricsForCluster(cluster, stackV4Response, policyHostGroupInstanceInfo.getPolicyHostGroup(),
                        pollingUserCrn, maxDecommissionNodeCount);
        metricService.recordYarnInvocation(cluster, start);

        if (cluster.getUpdateFailedDetails() != null && pollingUserCrn.equals(cluster.getMachineUserCrn())) {
            // Successful YARN API call
            LOGGER.info("Successfully polled YARN with machineUser: {}, resetting UpdateFailedDetails for cluster: {}", cluster.getMachineUserCrn(),
                    cluster.getStackCrn());
            clusterService.setUpdateFailedDetails(cluster.getId(), null);
        }

        int yarnRecommendedScaleUpCount = yarnResponseUtils.getYarnRecommendedScaleUpCount(yarnResponse, policyHostGroupInstanceInfo.getPolicyHostGroup());
        int finalScaleUpCount = IntStream.of(yarnRecommendedScaleUpCount,
                maxAllowedUpScale, loadAlertConfiguration.getMaxScaleUpStepSize()).min().getAsInt();

        int allowedDownscale = maxDecommissionNodeCount.orElse(maxAllowedDownScale);
        allowedDownscale = Math.min(allowedDownscale, loadAlertConfiguration.getMaxScaleDownStepSize());
        List<String> yarnRecommendedDecommissionHosts =
                yarnResponseUtils.getYarnRecommendedDecommissionHostsForHostGroup(yarnResponse, policyHostGroupInstanceInfo.getHostFqdnsToInstanceId());
        List<String> finalHostsToDecommission = yarnRecommendedDecommissionHosts.stream()
                .limit(allowedDownscale).collect(Collectors.toList());

        String yarnRecommendationMessage = messagesService.getMessageWithArgs(AUTOSCALE_YARN_RECOMMENDATION_SUCCESS, yarnRecommendedScaleUpCount,
                yarnRecommendedDecommissionHosts, finalScaleUpCount, finalHostsToDecommission);
        LOGGER.info("yarnRecommendedScaleUpCount={}, yarnRecommendedDecommission={}, determinedScaleUpCount (based on step-size and maxAllowedUpscale): {}, " +
                        "determinedDecommissionHosts (based on step-size and maxAllowedDownscale): {}",
                yarnRecommendedScaleUpCount, yarnRecommendedDecommissionHosts, finalScaleUpCount, finalHostsToDecommission);

        ScalingAdjustmentType adjustmentType = !stopStartEnabled ? REGULAR : STOPSTART;

        publishScalingEventIfNeeded(cluster, finalScaleUpCount, stackV4Response, finalHostsToDecommission, adjustmentType, policyHostGroupInstanceInfo,
                yarnRecommendationMessage);
    }

    protected boolean isCoolDownTimeElapsed(String clusterCrn, String coolDownAction, long expectedCoolDownMillis, long lastClusterScalingActivity) {
        long remainingTime = ClusterUtils.getRemainingCooldownTime(
                expectedCoolDownMillis, lastClusterScalingActivity);

        if (remainingTime <= 0) {
            return true;
        } else {
            LOGGER.debug("Cluster {} cannot be {} for {} min(s)", clusterCrn, coolDownAction,
                    format((double) remainingTime / TimeUtil.MIN_IN_MS));
        }
        return false;
    }

    private synchronized String format(double number) {
        return ClusterUtils.TIME_FORMAT.format(number);
    }

    private void publishScalingEventIfNeeded(Cluster cluster, int finalScaleUpCount, StackV4Response stackV4Response, List<String> hostsToDecommission,
            ScalingAdjustmentType adjustmentType, LoadAlertPolicyHostGroupInstanceInfo policyHostGroupInstanceInfo, String yarnRecommendationMessage) {
        LoadAlert loadAlert = policyHostGroupInstanceInfo.getLoadAlert();
        StringBuilder sb = new StringBuilder(yarnRecommendationMessage);
        if (upscalable(cluster, loadAlert.getLoadAlertConfiguration(), finalScaleUpCount)) {
            ScalingActivity scalingActivity =
                    scalingActivityService.create(cluster, METRICS_COLLECTION_SUCCESS, sb.append(UPSCALE_MESSAGE).toString(), clock.getCurrentTimeMillis());
            if (STOPSTART.equals(adjustmentType)) {
                Integer existingClusterNodeCount = stackV4Response.getNodeCount() - policyHostGroupInstanceInfo.getStoppedHostInstanceIds().size();
                eventSender.sendStopStartScaleUpEvent(loadAlert, existingClusterNodeCount, policyHostGroupInstanceInfo.getServicesHealthyInstanceIds().size(),
                        finalScaleUpCount, scalingActivity.getId());
            } else {
                eventSender.sendScaleUpEvent(loadAlert, stackV4Response.getNodeCount(), policyHostGroupInstanceInfo.getHostFqdnsToInstanceId().size(),
                        policyHostGroupInstanceInfo.getServicesHealthyInstanceIds().size(), finalScaleUpCount, scalingActivity.getId());
            }
        } else if (downscalable(cluster, loadAlert.getLoadAlertConfiguration(), hostsToDecommission)) {
            ScalingActivity scalingActivity = scalingActivityService.create(cluster, METRICS_COLLECTION_SUCCESS, sb.append(DOWNSCALE_MESSAGE).toString(),
                    clock.getCurrentTimeMillis());
            eventSender.sendScaleDownEvent(loadAlert, policyHostGroupInstanceInfo.getServicesHealthyInstanceIds().size(), hostsToDecommission,
                    policyHostGroupInstanceInfo.getServicesHealthyInstanceIds().size(), adjustmentType, scalingActivity.getId());
        }
    }

    private boolean upscalable(Cluster cluster, LoadAlertConfiguration loadAlertConfiguration, int finalScaleUpCount) {
        return finalScaleUpCount > 0 && isCoolDownTimeElapsed(cluster.getStackCrn(), "scaled-up",
                loadAlertConfiguration.getScaleUpCoolDownMillis(), cluster.getLastScalingActivity());
    }

    private boolean downscalable(Cluster cluster, LoadAlertConfiguration loadAlertConfiguration, List<String> hostsToDecommission) {
        return !hostsToDecommission.isEmpty() && isCoolDownTimeElapsed(cluster.getStackCrn(), "scaled-down",
                loadAlertConfiguration.getScaleDownCoolDownMillis(), cluster.getLastScalingActivity());
    }
}
