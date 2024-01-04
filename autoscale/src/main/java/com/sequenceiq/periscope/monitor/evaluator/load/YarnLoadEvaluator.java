package com.sequenceiq.periscope.monitor.evaluator.load;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.UpdateFailedDetails;
import com.sequenceiq.periscope.model.adjustment.MandatoryScalingAdjustmentParameters;
import com.sequenceiq.periscope.model.adjustment.RegularScalingAdjustmentParameters;
import com.sequenceiq.periscope.model.adjustment.StopStartScalingAdjustmentParameters;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RegularScalingAdjustmentService;
import com.sequenceiq.periscope.service.StopStartScalingAdjustmentService;
import com.sequenceiq.periscope.service.YarnBasedScalingAdjustmentService;
import com.sequenceiq.periscope.utils.LoggingUtils;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Component("YarnLoadEvaluator")
@Scope("prototype")
public class YarnLoadEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnLoadEvaluator.class);

    private static final String EVALUATOR_NAME = YarnLoadEvaluator.class.getName();

    private static final Long UPDATE_FAILED_INTERVAL_MINUTES = 60L;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RegularScalingAdjustmentService regularScalingAdjustmentService;

    @Inject
    private StopStartScalingAdjustmentService stopStartScalingAdjustmentService;

    @Inject
    private EventPublisher eventPublisher;

    @Inject
    private YarnBasedScalingAdjustmentService yarnBasedScalingAdjustmentService;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    private long clusterId;

    private Cluster cluster;

    private LoadAlert loadAlert;

    private LoadAlertConfiguration loadAlertConfiguration;

    private String policyHostGroup;

    private String pollingUserCrn;

    private boolean stopStartEnabled;

    @Nonnull
    @Override
    public EvaluatorContext getContext() {
        return new ClusterIdEvaluatorContext(clusterId);
    }

    @Override
    public void setContext(EvaluatorContext context) {
        clusterId = (long) context.getData();
    }

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    @Override
    protected void execute() {
        LOGGER.info("YarnLoadEvaluator executing for clusterId: {}", clusterId);
        long start = System.currentTimeMillis();
        String stackCrn = null;
        try {
            cluster = clusterService.findById(clusterId);
            LoggingUtils.buildMdcContext(cluster);
            stackCrn = cluster.getStackCrn();
            loadAlert = cluster.getLoadAlerts().iterator().next();
            loadAlertConfiguration = loadAlert.getLoadAlertConfiguration();
            policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();
            pollingUserCrn = Optional.ofNullable(getMachineUserCrnIfApplicable(cluster)).orElse(cluster.getClusterPertain().getUserCrn());
            stopStartEnabled = Boolean.TRUE.equals(cluster.isStopStartScalingEnabled());

            if (isCoolDownTimeElapsed(stackCrn, "polled", loadAlertConfiguration.getPollingCoolDownMillis(),
                    cluster.getLastScalingActivity())) {
                StackV4Response stackV4Response = cloudbreakCommunicator.getByCrn(stackCrn);

                int serviceHealthyHostGroupSize = stackResponseUtils.getCloudInstanceIdsWithServicesHealthyForHostGroup(stackV4Response, policyHostGroup).size();
                int existingHostGroupSize = stackResponseUtils.getCloudInstanceIdsForHostGroup(stackV4Response, policyHostGroup).size();

                MandatoryScalingAdjustmentParameters scalingAdjustmentParameters = getMandatoryAdjustmentParameters(stopStartEnabled, loadAlertConfiguration,
                        existingHostGroupSize, serviceHealthyHostGroupSize);

                if (mandatoryAdjustmentRequired(scalingAdjustmentParameters)) {
                    if (stopStartEnabled) {
                        stopStartScalingAdjustmentService.performMandatoryAdjustment(cluster, pollingUserCrn, stackV4Response, scalingAdjustmentParameters);
                    } else {
                        regularScalingAdjustmentService.performMandatoryAdjustment(cluster, pollingUserCrn, stackV4Response, scalingAdjustmentParameters);
                    }
                } else {
                    yarnBasedScalingAdjustmentService.pollYarnMetricsAndScaleCluster(cluster, pollingUserCrn, stopStartEnabled, stackV4Response);
                }
            }
        } catch (Exception ex) {
            LOGGER.info("Failed to process load alert for Cluster '{}', userCrn: {}, exception: ", stackCrn, pollingUserCrn, ex);
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId, ex, Instant.now().toEpochMilli(),
                    pollingUserCrn != null && pollingUserCrn.equals(cluster.getMachineUserCrn()), pollingUserCrn));
        } finally {
            LOGGER.debug("Finished loadEvaluator for cluster '{}' in '{}' ms", stackCrn, System.currentTimeMillis() - start);
        }
    }

    private MandatoryScalingAdjustmentParameters getMandatoryAdjustmentParameters(boolean stopStartEnabled,
            @Nonnull LoadAlertConfiguration loadAlertConfiguration, int existingHostGroupSize, int serviceHealthyHostGroupSize) {

        int maxResourceValueOffset = loadAlertConfiguration.getMaxResourceValue() - (stopStartEnabled ? serviceHealthyHostGroupSize : existingHostGroupSize);
        int minResourceValueOffset = serviceHealthyHostGroupSize - loadAlertConfiguration.getMinResourceValue();
        int stopStartAdjustment = loadAlertConfiguration.getMaxResourceValue() - existingHostGroupSize;

        if (stopStartEnabled) {
            StopStartScalingAdjustmentParameters stopStartAdjustmentParameters = new StopStartScalingAdjustmentParameters();
            stopStartAdjustmentParameters.setUpscaleAdjustment(Optional.of(stopStartAdjustment)
                    .filter(mandatoryStopStartUpscale -> mandatoryStopStartUpscale > 0).orElse(null));
            stopStartAdjustmentParameters.setDownscaleAdjustment(Optional.of(stopStartAdjustment)
                    .filter(mandatoryStopStartDownscale -> mandatoryStopStartDownscale < 0).map(Math::abs).orElse(null));
            return stopStartAdjustmentParameters;
        } else {
            RegularScalingAdjustmentParameters regularAdjustmentParameters = new RegularScalingAdjustmentParameters();
            regularAdjustmentParameters.setUpscaleAdjustment(Optional.of(minResourceValueOffset)
                    .filter(mandatoryUpScale -> mandatoryUpScale < 0).map(upscale -> -1 * upscale).orElse(null));
            regularAdjustmentParameters.setDownscaleAdjustment(Optional.of(maxResourceValueOffset)
                    .filter(mandatoryDownscale -> mandatoryDownscale < 0).map(downscale -> -1 * downscale).orElse(null));
            return regularAdjustmentParameters;
        }

    }

    private String getMachineUserCrnIfApplicable(Cluster cluster) {
        UpdateFailedDetails updateFailedDetails = cluster.getUpdateFailedDetails();
        if (updateFailedDetails == null || TimeUnit.MILLISECONDS.toMinutes(
                Instant.now().toEpochMilli() - updateFailedDetails.getLastExceptionTimestamp()) >= UPDATE_FAILED_INTERVAL_MINUTES) {
            if (cluster.getMachineUserCrn() != null) {
                LOGGER.debug("Attempting to invoke YARN with machineUser, forbiddenExceptionCount: {} for evaluator: {}",
                        updateFailedDetails != null ? updateFailedDetails.getExceptionCount() : 0, EVALUATOR_NAME);
            }
            return cluster.getMachineUserCrn();
        }
        return null;
    }

    private boolean mandatoryAdjustmentRequired(MandatoryScalingAdjustmentParameters scalingAdjustmentParameters) {
        return Objects.nonNull(scalingAdjustmentParameters.getUpscaleAdjustment()) || Objects.nonNull(scalingAdjustmentParameters.getDownscaleAdjustment());
    }

}
