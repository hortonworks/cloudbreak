package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.ActivityStatus.MANDATORY_DOWNSCALE;
import static com.sequenceiq.periscope.api.model.ActivityStatus.MANDATORY_UPSCALE;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_MANDATORY_DOWNSCALE;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_MANDATORY_UPSCALE;
import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.model.adjustment.MandatoryScalingAdjustmentParameters;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.evaluator.load.YarnResponseUtils;
import com.sequenceiq.periscope.monitor.sender.ScalingEventSender;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Service
public class RegularScalingAdjustmentService implements MandatoryScalingAdjustmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegularScalingAdjustmentService.class);

    @Inject
    private ScalingEventSender scalingEventSender;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private YarnMetricsClient yarnMetricsClient;

    @Inject
    private YarnResponseUtils yarnResponseUtils;

    @Inject
    private ScalingActivityService scalingActivityService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private Clock clock;

    @Override
    public void performMandatoryAdjustment(Cluster cluster, String pollingUserCrn, StackV4Response stackResponse,
            MandatoryScalingAdjustmentParameters scalingAdjustmentParameters) {
        LoadAlert loadAlert = cluster.getLoadAlerts().iterator().next();
        String policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();

        LoadAlertPolicyHostGroupInstanceInfo policyHostGroupInstanceInfo =
                new LoadAlertPolicyHostGroupInstanceInfo(loadAlert, policyHostGroup,
                        stackResponseUtils.getCloudInstanceIdsForHostGroup(stackResponse, policyHostGroup),
                        stackResponseUtils.getCloudInstanceIdsWithServicesHealthyForHostGroup(stackResponse, policyHostGroup),
                        stackResponseUtils.getStoppedCloudInstanceIdsInHostGroup(stackResponse, policyHostGroup));

        int existingClusterNodeCount = stackResponse.getNodeCount();

        publishScalingEventIfNeeded(cluster, existingClusterNodeCount, pollingUserCrn, stackResponse, scalingAdjustmentParameters, policyHostGroupInstanceInfo);
    }

    private void publishScalingEventIfNeeded(Cluster cluster, int existingClusterNodeCount, String pollingUserCrn, StackV4Response stackV4Response,
            MandatoryScalingAdjustmentParameters scalingAdjustmentParameters, LoadAlertPolicyHostGroupInstanceInfo policyHostGroupInstanceInfo) {
        LoadAlert loadAlert = policyHostGroupInstanceInfo.getLoadAlert();
        if (scalingAdjustmentParameters.getUpscaleAdjustment() != null) {
            Integer targetScaleUpCount = Math.min(scalingAdjustmentParameters.getUpscaleAdjustment(),
                    loadAlert.getLoadAlertConfiguration().getMaxScaleUpStepSize());
            String mandatoryUpscaleMsg = messagesService.getMessageWithArgs(AUTOSCALE_MANDATORY_UPSCALE, targetScaleUpCount, REGULAR);
            ScalingActivity scalingActivity = scalingActivityService.create(cluster, MANDATORY_UPSCALE, mandatoryUpscaleMsg, clock.getCurrentTimeMillis());
            scalingEventSender.sendScaleUpEvent(loadAlert, existingClusterNodeCount, policyHostGroupInstanceInfo.getHostFqdnsToInstanceId().size(),
                    policyHostGroupInstanceInfo.getServicesHealthyInstanceIds().size(),
                    targetScaleUpCount, scalingActivity.getId());
            LOGGER.info("Triggered mandatory adjustment ScaleUp for Cluster '{}', NodeCount '{}', HostGroup '{}'", cluster.getStackCrn(),
                    targetScaleUpCount, policyHostGroupInstanceInfo.getPolicyHostGroup());
        } else if (scalingAdjustmentParameters.getDownscaleAdjustment() != null) {
            List<String> hostsToDecommission = collectRegularDownscaleRecommendations(cluster,
                    pollingUserCrn, stackV4Response, scalingAdjustmentParameters.getDownscaleAdjustment(), policyHostGroupInstanceInfo);
            String mandatoryDownscaleMsg = messagesService.getMessageWithArgs(AUTOSCALE_MANDATORY_DOWNSCALE, hostsToDecommission, REGULAR);
            ScalingActivity scalingActivity = scalingActivityService.create(cluster, MANDATORY_DOWNSCALE, mandatoryDownscaleMsg, clock.getCurrentTimeMillis());
            scalingEventSender.sendScaleDownEvent(loadAlert, policyHostGroupInstanceInfo.getHostFqdnsToInstanceId().size(), hostsToDecommission,
                    policyHostGroupInstanceInfo.getServicesHealthyInstanceIds().size(), REGULAR, scalingActivity.getId());
            LOGGER.info("Triggered mandatory adjustment ScaleDown for Cluster '{}', HostsToDecommission '{}', HostGroup '{}'",
                    cluster.getStackCrn(), hostsToDecommission, policyHostGroupInstanceInfo.getPolicyHostGroup());
        }
    }

    private List<String> collectRegularDownscaleRecommendations(Cluster cluster, String pollingUserCrn,
            StackV4Response stackResponse, Integer mandatoryDownscaleCount, LoadAlertPolicyHostGroupInstanceInfo policyHostGroupInstanceInfo) {
        try {
            LoadAlert loadAlert = policyHostGroupInstanceInfo.getLoadAlert();
            int maxAllowedDownscale =
                    Math.max(0, policyHostGroupInstanceInfo.getHostFqdnsToInstanceId().size() - loadAlert.getLoadAlertConfiguration().getMinResourceValue());
            int allowedDownscale = IntStream.of(mandatoryDownscaleCount, maxAllowedDownscale,
                    loadAlert.getLoadAlertConfiguration().getMaxScaleDownStepSize()).min().getAsInt();

            YarnScalingServiceV1Response yarnResponse = yarnMetricsClient.getYarnMetricsForCluster(cluster, stackResponse,
                    policyHostGroupInstanceInfo.getPolicyHostGroup(), pollingUserCrn, Optional.of(mandatoryDownscaleCount));
            List<String> yarnRecommendedDecommissionHosts =
                    yarnResponseUtils.getYarnRecommendedDecommissionHostsForHostGroup(yarnResponse, policyHostGroupInstanceInfo.getHostFqdnsToInstanceId());
            return yarnRecommendedDecommissionHosts.stream()
                    .limit(allowedDownscale)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Error when invoking YARN for mandatory regular downscale recommendations for cluster '{}', hostGroup '{}'", cluster.getStackCrn(),
                    policyHostGroupInstanceInfo.getPolicyHostGroup(), e);
            throw new RuntimeException(e);
        }
    }

}
