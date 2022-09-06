package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;
import static com.sequenceiq.periscope.model.ScalingAdjustmentType.STOPSTART;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
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

    @Inject
    private YarnResponseUtils yarnResponseUtils;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private YarnMetricsClient yarnMetricsClient;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ScalingEventSender eventSender;

    private LoadAlert loadAlert;

    private LoadAlertConfiguration loadAlertConfiguration;

    private String policyHostGroup;

    private Map<String, String> hostFqdnsToInstanceId;

    private List<String> servicesHealthyInstanceIds;

    private List<String> stoppedHostInstanceIds;

    public void pollYarnMetricsAndScaleCluster(Cluster cluster, String pollingUserCrn, boolean stopStartEnabled, StackV4Response stackV4Response)
            throws Exception {

        loadAlert = cluster.getLoadAlerts().iterator().next();
        policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();
        loadAlertConfiguration = loadAlert.getLoadAlertConfiguration();

        hostFqdnsToInstanceId = stackResponseUtils.getCloudInstanceIdsForHostGroup(stackV4Response, policyHostGroup);
        servicesHealthyInstanceIds = stackResponseUtils.getCloudInstanceIdsWithServicesHealthyForHostGroup(stackV4Response, policyHostGroup);
        stoppedHostInstanceIds = stackResponseUtils.getStoppedCloudInstanceIdsInHostGroup(stackV4Response, policyHostGroup);

        int serviceHealthyHostGroupSize = servicesHealthyInstanceIds.size();
        int existingHostGroupSize = hostFqdnsToInstanceId.size();

        int maxResourceValueOffset = loadAlertConfiguration.getMaxResourceValue() - (stopStartEnabled ? serviceHealthyHostGroupSize : existingHostGroupSize);
        int minResourceValueOffset = serviceHealthyHostGroupSize - loadAlertConfiguration.getMinResourceValue();

        int maxAllowedUpScale = Math.max(maxResourceValueOffset, 0);
        int maxAllowedDownScale = Math.max(minResourceValueOffset, 0);

        LOGGER.info("Various counts: hostFqdnsToInstanceId={}, servicesHealthyHostFqdnsToInstanceId: {}, stoppedHostFqdnsToInstanceId: {}, " +
                        "maxResourceValueOffset={}, minResourceValueOffset={}, maxAllowedUpScale={}, maxAllowedDownScale={}",
                existingHostGroupSize, serviceHealthyHostGroupSize, stoppedHostInstanceIds.size(),
                maxResourceValueOffset, minResourceValueOffset, maxAllowedUpScale, maxAllowedDownScale);

        Optional<Integer> maxDecommissionNodeCount = Optional.of(maxResourceValueOffset)
                .filter(mandatoryDownscale -> mandatoryDownscale < 0).map(downscale -> -1 * downscale);

        YarnScalingServiceV1Response yarnResponse =
                yarnMetricsClient.getYarnMetricsForCluster(cluster, stackV4Response, policyHostGroup, pollingUserCrn, maxDecommissionNodeCount);

        if (cluster.getUpdateFailedDetails() != null && pollingUserCrn.equals(cluster.getMachineUserCrn())) {
            // Successful YARN API call
            clusterService.setUpdateFailedDetails(cluster.getId(), null);
        }

        int yarnRecommendedScaleUpCount = yarnResponseUtils.getYarnRecommendedScaleUpCount(yarnResponse, policyHostGroup);
        int finalScaleUpCount = IntStream.of(yarnRecommendedScaleUpCount,
                maxAllowedUpScale, loadAlertConfiguration.getMaxScaleUpStepSize()).min().getAsInt();

        int allowedDownscale = maxDecommissionNodeCount.orElse(maxAllowedDownScale);
        allowedDownscale = Math.min(allowedDownscale, loadAlertConfiguration.getMaxScaleDownStepSize());
        List<String> yarnRecommendedDecommissionHosts =
                yarnResponseUtils.getYarnRecommendedDecommissionHostsForHostGroup(yarnResponse, hostFqdnsToInstanceId);
        List<String> finalHostsToDecommission = yarnRecommendedDecommissionHosts.stream()
                .limit(allowedDownscale).collect(Collectors.toList());

        LOGGER.info("yarnRecommendedScaleUpCount={}, yarnRecommendedDecommission={}, determinedScaleUpCount (based on step-size and maxAllowedUpscale): {}, " +
                        "determinedDecommissionHosts (based on step-size and maxAllowedDownscale): {}",
                yarnRecommendedScaleUpCount, yarnRecommendedDecommissionHosts, finalScaleUpCount, finalHostsToDecommission);

        ScalingAdjustmentType adjustmentType = !stopStartEnabled ? REGULAR : STOPSTART;

        publishScalingEventIfNeeded(cluster, finalScaleUpCount, stackV4Response, finalHostsToDecommission, adjustmentType);
    }

    protected boolean isCoolDownTimeElapsed(String clusterCrn, String coolDownAction, long expectedCoolDownMillis, long lastClusterScalingActivity) {
        long remainingTime = ClusterUtils.getRemainingCooldownTime(
                expectedCoolDownMillis, lastClusterScalingActivity);

        if (remainingTime <= 0) {
            return true;
        } else {
            LOGGER.debug("Cluster {} cannot be {} for {} min(s)", clusterCrn, coolDownAction,
                    ClusterUtils.TIME_FORMAT.format((double) remainingTime / TimeUtil.MIN_IN_MS));
        }
        return false;
    }

    private void publishScalingEventIfNeeded(Cluster cluster, int finalScaleUpCount, StackV4Response stackV4Response, List<String> hostsToDecommission,
            ScalingAdjustmentType adjustmentType) {
        if (upscalable(cluster, finalScaleUpCount))  {
            if (STOPSTART.equals(adjustmentType)) {
                Integer existingClusterNodeCount = stackV4Response.getNodeCount() - stoppedHostInstanceIds.size();
                eventSender.sendStopStartScaleUpEvent(loadAlert, existingClusterNodeCount, servicesHealthyInstanceIds.size(), finalScaleUpCount);
            } else {
                eventSender.sendScaleUpEvent(loadAlert, stackV4Response.getNodeCount(), hostFqdnsToInstanceId.size(), servicesHealthyInstanceIds.size(),
                        finalScaleUpCount);
            }
        } else if (downscalable(cluster, hostsToDecommission)) {
            eventSender.sendScaleDownEvent(loadAlert, servicesHealthyInstanceIds.size(), hostsToDecommission, servicesHealthyInstanceIds.size(),
                    adjustmentType);
        }
    }

    private boolean upscalable(Cluster cluster, int finalScaleUpCount) {
        return finalScaleUpCount > 0 && isCoolDownTimeElapsed(cluster.getStackCrn(), "scaled-up",
                loadAlertConfiguration.getScaleUpCoolDownMillis(), cluster.getLastScalingActivity());
    }

    private boolean downscalable(Cluster cluster, List<String> hostsToDecommission) {
        return !hostsToDecommission.isEmpty() && isCoolDownTimeElapsed(cluster.getStackCrn(), "scaled-down",
                loadAlertConfiguration.getScaleDownCoolDownMillis(), cluster.getLastScalingActivity());
    }

}
