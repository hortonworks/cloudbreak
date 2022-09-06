package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;

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

    private LoadAlert loadAlert;

    private LoadAlertConfiguration loadAlertConfiguration;

    private String policyHostGroup;

    private Map<String, String> hostFqdnsToInstanceId;

    private List<String> servicesHealthyHostInstanceIds;

    @Override
    public void performMandatoryAdjustment(Cluster cluster, String pollingUserCrn, StackV4Response stackResponse,
            MandatoryScalingAdjustmentParameters scalingAdjustmentParameters) {
        loadAlert = cluster.getLoadAlerts().iterator().next();
        loadAlertConfiguration = loadAlert.getLoadAlertConfiguration();
        policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();

        hostFqdnsToInstanceId = stackResponseUtils.getCloudInstanceIdsForHostGroup(stackResponse, policyHostGroup);
        servicesHealthyHostInstanceIds = stackResponseUtils.getCloudInstanceIdsWithServicesHealthyForHostGroup(stackResponse,
                policyHostGroup);

        int existingClusterNodeCount = stackResponse.getNodeCount();

        publishScalingEventIfNeeded(cluster, existingClusterNodeCount, pollingUserCrn, stackResponse, scalingAdjustmentParameters);
    }

    private void publishScalingEventIfNeeded(Cluster cluster, int existingClusterNodeCount, String pollingUserCrn, StackV4Response stackV4Response,
            MandatoryScalingAdjustmentParameters scalingAdjustmentParameters) {

        if (scalingAdjustmentParameters.getUpscaleAdjustment() != null) {
            Integer targetScaleUpCount = Math.min(scalingAdjustmentParameters.getUpscaleAdjustment(), loadAlertConfiguration.getMaxScaleUpStepSize());

            scalingEventSender.sendScaleUpEvent(loadAlert, existingClusterNodeCount, hostFqdnsToInstanceId.size(), servicesHealthyHostInstanceIds.size(),
                    targetScaleUpCount);
            LOGGER.info("Triggered mandatory adjustment ScaleUp for Cluster '{}', NodeCount '{}', HostGroup '{}'", cluster.getStackCrn(),
                    targetScaleUpCount, policyHostGroup);
        } else if (scalingAdjustmentParameters.getDownscaleAdjustment() != null) {
            List<String> hostsToDecommission = collectRegularDownscaleRecommendations(cluster, hostFqdnsToInstanceId, pollingUserCrn, stackV4Response,
                    scalingAdjustmentParameters.getDownscaleAdjustment());

            scalingEventSender.sendScaleDownEvent(loadAlert, hostFqdnsToInstanceId.size(), hostsToDecommission, servicesHealthyHostInstanceIds.size(), REGULAR);
            LOGGER.info("Triggered mandatory adjustment ScaleDown for Cluster '{}', HostsToDecommission '{}', HostGroup '{}'",
                    cluster.getStackCrn(), hostsToDecommission, policyHostGroup);
        }
    }

    private List<String> collectRegularDownscaleRecommendations(Cluster cluster, Map<String, String> hostFqdnsToInstanceId, String pollingUserCrn,
            StackV4Response stackResponse, Integer mandatoryDownscaleCount) {
        try {
            int maxAllowedDownscale = Math.max(0, hostFqdnsToInstanceId.size() - loadAlertConfiguration.getMinResourceValue());
            int allowedDownscale = IntStream.of(mandatoryDownscaleCount, maxAllowedDownscale,
                    loadAlertConfiguration.getMaxScaleDownStepSize()).min().getAsInt();

            YarnScalingServiceV1Response yarnResponse = yarnMetricsClient.getYarnMetricsForCluster(cluster, stackResponse, policyHostGroup,
                    pollingUserCrn, Optional.of(mandatoryDownscaleCount));
            List<String> yarnRecommendedDecommissionHosts =
                    yarnResponseUtils.getYarnRecommendedDecommissionHostsForHostGroup(yarnResponse, hostFqdnsToInstanceId);
            return yarnRecommendedDecommissionHosts.stream()
                    .limit(allowedDownscale)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Error when invoking YARN for mandatory regular downscale recommendations for cluster '{}', hostGroup '{}'", cluster.getStackCrn(),
                    policyHostGroup, e);
            throw new RuntimeException(e);
        }
    }

}
