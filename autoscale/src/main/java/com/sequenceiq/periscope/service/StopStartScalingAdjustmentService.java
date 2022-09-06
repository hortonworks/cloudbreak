package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
public class StopStartScalingAdjustmentService implements MandatoryScalingAdjustmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartScalingAdjustmentService.class);

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

    private List<String> stoppedHostInstanceIds;

    @Override
    public void performMandatoryAdjustment(Cluster cluster, String pollingUserCrn, StackV4Response stackResponse,
            MandatoryScalingAdjustmentParameters scalingAdjustmentParameters) {

        loadAlert = cluster.getLoadAlerts().iterator().next();
        loadAlertConfiguration = loadAlert.getLoadAlertConfiguration();
        policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();

        hostFqdnsToInstanceId = stackResponseUtils.getCloudInstanceIdsForHostGroup(stackResponse, policyHostGroup);
        servicesHealthyHostInstanceIds = stackResponseUtils.getCloudInstanceIdsWithServicesHealthyForHostGroup(stackResponse,
                policyHostGroup);
        stoppedHostInstanceIds = stackResponseUtils.getStoppedCloudInstanceIdsInHostGroup(stackResponse, policyHostGroup);

        int stoppedNodeCount = stoppedHostInstanceIds.size();
        int existingClusterNodeCount = stackResponse.getNodeCount() - stoppedNodeCount;

        publishScalingEventIfNeeded(cluster, existingClusterNodeCount, pollingUserCrn, stackResponse, scalingAdjustmentParameters);
    }

    private void publishScalingEventIfNeeded(Cluster cluster, int existingClusterNodeCount, String pollingUserCrn, StackV4Response stackV4Response,
            MandatoryScalingAdjustmentParameters scalingAdjustmentParameters) {

        if (scalingAdjustmentParameters.getUpscaleAdjustment() != null) {
            Integer targetScaleUpCount = Math.min(scalingAdjustmentParameters.getUpscaleAdjustment(), loadAlertConfiguration.getMaxScaleUpStepSize());
            if (stoppedHostInstanceIds.isEmpty()) {
                scalingEventSender.sendScaleUpEvent(loadAlert, existingClusterNodeCount, hostFqdnsToInstanceId.size(), servicesHealthyHostInstanceIds.size(),
                        targetScaleUpCount);
                LOGGER.info("Triggered mandatory adjustment ScaleUp for Cluster '{}', NodeCount '{}', HostGroup '{}'",
                        cluster.getStackCrn(), targetScaleUpCount, policyHostGroup);
            } else {
                scalingEventSender.sendStopStartScaleUpEvent(loadAlert, existingClusterNodeCount, servicesHealthyHostInstanceIds.size(),
                        stoppedHostInstanceIds.size());
                LOGGER.info("Triggered mandatory adjustment stop-start ScaleUp for Cluster '{}', NodeCount '{}', HostGroup '{}'",
                        cluster.getStackCrn(), stoppedHostInstanceIds.size(), policyHostGroup);
            }
        } else if (scalingAdjustmentParameters.getDownscaleAdjustment() != null) {
            List<String> hostsToDecommission = collectStopStartDownscaleRecommendations(cluster, hostFqdnsToInstanceId, stoppedHostInstanceIds,
                    pollingUserCrn, stackV4Response, scalingAdjustmentParameters.getDownscaleAdjustment());

            scalingEventSender.sendScaleDownEvent(loadAlert, hostFqdnsToInstanceId.size(), hostsToDecommission, servicesHealthyHostInstanceIds.size(), REGULAR);
            LOGGER.info("Triggered mandatory adjustment ScaleDown for Cluster '{}', HostsToDecommission '{}', HostGroup '{}'",
                    cluster.getStackCrn(), hostsToDecommission, policyHostGroup);
        }
    }

    private List<String> collectStopStartDownscaleRecommendations(Cluster cluster, Map<String, String> hostFqdnsToInstanceId,
            List<String> stoppedHostIds, String pollingUserCrn, StackV4Response stackResponse, Integer mandatoryDownscaleCount) {
        try {
            int maxAllowedDownscale = Math.max(0, hostFqdnsToInstanceId.size() - loadAlertConfiguration.getMinResourceValue());
            int allowedDownscale = IntStream.of(mandatoryDownscaleCount, maxAllowedDownscale,
                            loadAlertConfiguration.getMaxScaleDownStepSize()).min().getAsInt();

            List<String> yarnRecommendedDecommissionHosts = emptyList();

            if (stoppedHostIds.size() < allowedDownscale) {
                YarnScalingServiceV1Response yarnResponse = yarnMetricsClient.getYarnMetricsForCluster(cluster, stackResponse, policyHostGroup,
                        pollingUserCrn, Optional.of(mandatoryDownscaleCount));
                yarnRecommendedDecommissionHosts =
                        yarnResponseUtils.getYarnRecommendedDecommissionHostsForHostGroup(yarnResponse, hostFqdnsToInstanceId);
            }

            return Stream.concat(stoppedHostIds.stream(), yarnRecommendedDecommissionHosts.stream())
                    .limit(allowedDownscale)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Error when invoking YARN for mandatory stop-start downscale recommendations for cluster '{}', hostGroup '{}'", cluster.getStackCrn(),
                    policyHostGroup, e);
            throw new RuntimeException(e);
        }
    }

}
