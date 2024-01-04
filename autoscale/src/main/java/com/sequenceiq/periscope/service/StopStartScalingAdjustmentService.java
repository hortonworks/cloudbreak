package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.ActivityStatus.MANDATORY_DOWNSCALE;
import static com.sequenceiq.periscope.api.model.ActivityStatus.MANDATORY_UPSCALE;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_MANDATORY_DOWNSCALE;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_MANDATORY_UPSCALE;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_STOPSTART_INITIAL_UPSCALE;
import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;
import static com.sequenceiq.periscope.model.ScalingAdjustmentType.STOPSTART;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.model.adjustment.MandatoryScalingAdjustmentParameters;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.evaluator.load.YarnResponseUtils;
import com.sequenceiq.periscope.monitor.sender.ScalingEventSender;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Service
public class StopStartScalingAdjustmentService implements MandatoryScalingAdjustmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartScalingAdjustmentService.class);

    @Inject
    private ScalingEventSender scalingEventSender;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private HistoryService historyService;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private YarnMetricsClient yarnMetricsClient;

    @Inject
    private YarnResponseUtils yarnResponseUtils;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private Clock clock;

    @Inject
    private ScalingActivityService scalingActivityService;

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

        int stoppedNodeCount = policyHostGroupInstanceInfo.getStoppedHostInstanceIds().size();
        int existingClusterNodeCount = stackResponse.getNodeCount() - stoppedNodeCount;

        publishScalingEventIfNeeded(cluster, existingClusterNodeCount, pollingUserCrn, stackResponse, scalingAdjustmentParameters, policyHostGroupInstanceInfo);
    }

    private void publishScalingEventIfNeeded(Cluster cluster, int existingClusterNodeCount, String pollingUserCrn, StackV4Response stackV4Response,
            MandatoryScalingAdjustmentParameters scalingAdjustmentParameters, LoadAlertPolicyHostGroupInstanceInfo policyHostGroupInstanceInfo) {
        LoadAlert loadAlert = policyHostGroupInstanceInfo.getLoadAlert();
        if (scalingAdjustmentParameters.getUpscaleAdjustment() != null) {
            Integer targetScaleUpCount = Math.min(scalingAdjustmentParameters.getUpscaleAdjustment(),
                    loadAlert.getLoadAlertConfiguration().getMaxScaleUpStepSize());
            sendHistoryUpdateForMaxNodeCountAdjustment(cluster, loadAlert.getLoadAlertConfiguration().getMaxResourceValue(),
                    policyHostGroupInstanceInfo.getPolicyHostGroup());

            String mandatoryUpscaleMsg;
            ScalingActivity scalingActivity;
            if (policyHostGroupInstanceInfo.getStoppedHostInstanceIds().isEmpty()) {
                mandatoryUpscaleMsg = messagesService.getMessageWithArgs(AUTOSCALE_MANDATORY_UPSCALE, targetScaleUpCount, REGULAR);
                scalingActivity = scalingActivityService.create(cluster, MANDATORY_UPSCALE, mandatoryUpscaleMsg, clock.getCurrentTimeMillis());
                scalingEventSender.sendScaleUpEvent(loadAlert, existingClusterNodeCount, policyHostGroupInstanceInfo.getHostFqdnsToInstanceId().size(),
                        policyHostGroupInstanceInfo.getServicesHealthyInstanceIds().size(),
                        targetScaleUpCount, scalingActivity.getId());
                LOGGER.info("Triggered mandatory adjustment ScaleUp for Cluster '{}', NodeCount '{}', HostGroup '{}'",
                        cluster.getStackCrn(), targetScaleUpCount, policyHostGroupInstanceInfo.getPolicyHostGroup());
            } else {
                mandatoryUpscaleMsg = messagesService.getMessageWithArgs(AUTOSCALE_MANDATORY_UPSCALE, targetScaleUpCount, STOPSTART);
                scalingActivity = scalingActivityService.create(cluster, MANDATORY_UPSCALE, mandatoryUpscaleMsg, clock.getCurrentTimeMillis());
                scalingEventSender.sendStopStartScaleUpEvent(loadAlert, existingClusterNodeCount,
                        policyHostGroupInstanceInfo.getServicesHealthyInstanceIds().size(), policyHostGroupInstanceInfo.getStoppedHostInstanceIds().size(),
                        scalingActivity.getId());
                LOGGER.info("Triggered mandatory adjustment stop-start ScaleUp for Cluster '{}', NodeCount '{}', HostGroup '{}'",
                        cluster.getStackCrn(), policyHostGroupInstanceInfo.getStoppedHostInstanceIds().size(),
                        policyHostGroupInstanceInfo.getPolicyHostGroup());
            }
        } else if (scalingAdjustmentParameters.getDownscaleAdjustment() != null) {
            List<String> hostsToDecommission = collectStopStartDownscaleRecommendations(cluster, policyHostGroupInstanceInfo,
                    pollingUserCrn, stackV4Response, scalingAdjustmentParameters.getDownscaleAdjustment());
            sendHistoryUpdateForMaxNodeCountAdjustment(cluster, loadAlert.getLoadAlertConfiguration().getMaxResourceValue(),
                    policyHostGroupInstanceInfo.getPolicyHostGroup());

            String mandatoryDownscaleMsg = messagesService.getMessageWithArgs(AUTOSCALE_MANDATORY_DOWNSCALE, hostsToDecommission, REGULAR);
            ScalingActivity scalingActivity = scalingActivityService.create(cluster, MANDATORY_DOWNSCALE, mandatoryDownscaleMsg, clock.getCurrentTimeMillis());
            scalingEventSender.sendScaleDownEvent(loadAlert, policyHostGroupInstanceInfo.getHostFqdnsToInstanceId().size(), hostsToDecommission,
                    policyHostGroupInstanceInfo.getServicesHealthyInstanceIds().size(), REGULAR, scalingActivity.getId());
            LOGGER.info("Triggered mandatory adjustment ScaleDown for Cluster '{}', HostsToDecommission '{}', HostGroup '{}'",
                    cluster.getStackCrn(), hostsToDecommission, policyHostGroupInstanceInfo.getPolicyHostGroup());
        }
    }

    private void sendHistoryUpdateForMaxNodeCountAdjustment(Cluster cluster, int maxResourceValue, String policyHostGroup) {
        History history = historyService.createEntry(ScalingStatus.TRIGGER_INFO,
                messagesService.getMessageWithArgs(AUTOSCALE_STOPSTART_INITIAL_UPSCALE, maxResourceValue, policyHostGroup), cluster);
        notificationSender.sendHistoryUpdateNotification(history, cluster);
    }

    private List<String> collectStopStartDownscaleRecommendations(Cluster cluster, LoadAlertPolicyHostGroupInstanceInfo policyHostGroupInstanceInfo,
            String pollingUserCrn, StackV4Response stackResponse, Integer mandatoryDownscaleCount) {
        try {
            LoadAlertConfiguration loadAlertConfiguration = policyHostGroupInstanceInfo.getLoadAlert().getLoadAlertConfiguration();
            int maxAllowedDownscale = Math.max(0, policyHostGroupInstanceInfo.getHostFqdnsToInstanceId().size() - loadAlertConfiguration.getMinResourceValue());
            int allowedDownscale = IntStream.of(mandatoryDownscaleCount, maxAllowedDownscale,
                            loadAlertConfiguration.getMaxScaleDownStepSize()).min().getAsInt();

            List<String> yarnRecommendedDecommissionHosts = emptyList();

            if (policyHostGroupInstanceInfo.getStoppedHostInstanceIds().size() < allowedDownscale) {
                YarnScalingServiceV1Response yarnResponse = yarnMetricsClient.getYarnMetricsForCluster(cluster,
                        stackResponse, policyHostGroupInstanceInfo.getPolicyHostGroup(), pollingUserCrn, Optional.of(mandatoryDownscaleCount));
                yarnRecommendedDecommissionHosts =
                        yarnResponseUtils.getYarnRecommendedDecommissionHostsForHostGroup(yarnResponse, policyHostGroupInstanceInfo.getHostFqdnsToInstanceId());
            }

            return Stream.concat(policyHostGroupInstanceInfo.getStoppedHostInstanceIds().stream(), yarnRecommendedDecommissionHosts.stream())
                    .limit(allowedDownscale)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Error when invoking YARN for mandatory stop-start downscale recommendations for cluster '{}', hostGroup '{}'", cluster.getStackCrn(),
                    policyHostGroupInstanceInfo.getPolicyHostGroup(), e);
            throw new RuntimeException(e);
        }
    }
}
