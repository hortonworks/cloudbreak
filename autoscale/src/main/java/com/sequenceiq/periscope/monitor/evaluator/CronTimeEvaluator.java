package com.sequenceiq.periscope.monitor.evaluator;

import static com.sequenceiq.periscope.api.model.ActivityStatus.METRICS_COLLECTION_FAILED;
import static com.sequenceiq.periscope.api.model.ActivityStatus.SCHEDULE_BASED_DOWNSCALE;
import static com.sequenceiq.periscope.api.model.ActivityStatus.SCHEDULE_BASED_UPSCALE;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_SCHEDULE_BASED_DOWNSCALE;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_SCHEDULE_BASED_UPSCALE;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_YARN_RECOMMENDATION_FAILED;
import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;
import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_MAX_SCALE_DOWN_STEP_SIZE;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.controller.validation.AlertValidator;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.domain.UpdateFailedDetails;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.monitor.MonitorUpdateRate;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.load.YarnResponseUtils;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.DependentHostGroupsService;
import com.sequenceiq.periscope.service.EntitlementValidationService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.ScalingActivityService;
import com.sequenceiq.periscope.utils.LoggingUtils;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Component("CronTimeEvaluator")
@Scope("prototype")
public class CronTimeEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CronTimeEvaluator.class);

    private static final String EVALUATOR_NAME = CronTimeEvaluator.class.getName();

    private static final Long UPDATE_FAILED_INTERVAL_MINUTES = 60L;

    private static final String YARN = "YARN";

    private static final String IMPALA = "IMPALA";

    @Inject
    private TimeAlertRepository alertRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private DateService dateService;

    @Inject
    private HistoryService historyService;

    @Inject
    private EventPublisher eventPublisher;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private YarnMetricsClient yarnMetricsClient;

    @Inject
    private YarnResponseUtils yarnResponseUtils;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private ScalingActivityService scalingActivityService;

    @Inject
    private ScalingPolicyTargetCalculator scalingPolicyTargetCalculator;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private AlertValidator alertValidator;

    @Inject
    private Clock clock;

    private long clusterId;

    @Override
    public void setContext(EvaluatorContext context) {
        clusterId = (long) context.getData();
    }

    @Override
    @Nonnull
    public EvaluatorContext getContext() {
        return new ClusterIdEvaluatorContext(clusterId);
    }

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    private boolean isTrigger(TimeAlert alert) {
        return dateService.isTrigger(alert, MonitorUpdateRate.CRON_UPDATE_RATE_IN_MILLIS);
    }

    private boolean isTrigger(TimeAlert alert, ZonedDateTime zdt) {
        return dateService.isTrigger(alert, MonitorUpdateRate.CRON_UPDATE_RATE_IN_MILLIS, zdt);
    }

    private boolean isPolicyAttached(BaseAlert alert) {
        return alert.getScalingPolicy() != null;
    }

    @Override
    public void execute() {
        long start = System.currentTimeMillis();
        Cluster cluster = clusterService.findById(clusterId);
        LoggingUtils.buildMdcContext(cluster);
        publishIfNeeded(alertRepository.findAllByClusterIdOrderById(clusterId));
        LOGGER.debug("Finished cronTimeEvaluator for cluster {} in {} ms", cluster.getStackCrn(), System.currentTimeMillis() - start);
    }

    protected void publishIfNeeded(List<TimeAlert> alerts) {
        TimeAlert triggeredAlert = null;
        for (TimeAlert alert : alerts) {
            boolean alertTriggerable = isTrigger(alert);
            if (isPolicyAttached(alert) && alertTriggerable && null == triggeredAlert) {
                publish(alert);
                triggeredAlert = alert;
            } else if (alertTriggerable && triggeredAlert != null) {
                historyService.createEntry(ScalingStatus.TRIGGER_FAILED,
                        messagesService.getMessage(MessageCode.SCHEDULE_CONFIG_OVERLAPS, List.of(alert.getName(), triggeredAlert.getName())),
                        alert.getCluster());
            }
        }
    }

    public void publishIfNeeded(Map<TimeAlert, ZonedDateTime> alerts) {
        for (Entry<TimeAlert, ZonedDateTime> alertEntry : alerts.entrySet()) {
            TimeAlert alert = alertEntry.getKey();
            if (isPolicyAttached(alert) && isTrigger(alert, alertEntry.getValue())) {
                publish(alert);
                break;
            }
        }
    }

    private void publish(TimeAlert alert) {
        ScalingEvent event = new ScalingEvent(alert);
        String scalingActivityMsg;

        StackV4Response stackV4Response = cloudbreakCommunicator.getByCrn(alert.getCluster().getStackCrn());
        int hostGroupNodeCount = stackResponseUtils.getNodeCountForHostGroup(stackV4Response, alert.getScalingPolicy().getHostGroup());
        int desiredAbsoluteNodeCount = scalingPolicyTargetCalculator.getDesiredAbsoluteNodeCount(event, hostGroupNodeCount);
        int targetIncrementNodeCount = desiredAbsoluteNodeCount - hostGroupNodeCount;
        ActivityStatus status = SCHEDULE_BASED_UPSCALE;

        event.setExistingHostGroupNodeCount(hostGroupNodeCount);
        event.setDesiredAbsoluteHostGroupNodeCount(desiredAbsoluteNodeCount);
        event.setExistingClusterNodeCount(stackV4Response.getNodeCount());
        event.setScalingAdjustmentType(REGULAR);
        scalingActivityMsg = messagesService.getMessageWithArgs(AUTOSCALE_SCHEDULE_BASED_UPSCALE, targetIncrementNodeCount);

        Set<String> servicesRunningOnHostgroup = cloudbreakCommunicator.getServicesRunningOnHostGroup(
                alert.getCluster().getStackCrn(), alert.getScalingPolicy().getHostGroup()).getServicesOnHostGroup();
        if (targetIncrementNodeCount < 0) {
            populateDecommissionCandidates(event, stackV4Response, alert.getCluster(),
                    alert.getScalingPolicy(), -targetIncrementNodeCount, servicesRunningOnHostgroup);
            status = SCHEDULE_BASED_DOWNSCALE;
            scalingActivityMsg = messagesService.getMessageWithArgs(AUTOSCALE_SCHEDULE_BASED_DOWNSCALE, event.getDecommissionNodeIds());
        }
        ScalingActivity scalingActivity = scalingActivityService.create(alert.getCluster(), status, scalingActivityMsg, clock.getCurrentTimeMillis());
        event.setActivityId(scalingActivity.getId());
        if (servicesRunningOnHostgroup.contains(IMPALA)) {
            if (!servicesRunningOnHostgroup.contains(YARN)) {
                alertValidator.validateImpalaScheduleBasedScalingEntitlement(stackV4Response, alert.getCluster().getStackCrn());
            }
        }
        eventPublisher.publishEvent(event);
        LOGGER.debug("Time alert '{}' triggered  for cluster '{}'", alert.getName(), alert.getCluster().getStackCrn());
    }

    private void populateDecommissionCandidates(ScalingEvent event, StackV4Response stackV4Response, Cluster cluster,
        ScalingPolicy policy, int mandatoryDownScaleCount, Set<String> servicesRunningOnHostgroup) {
        try {
            List<String> decommissionNodes = Collections.emptyList();

            if (servicesRunningOnHostgroup.contains(YARN)) {
                String pollingUserCrn = Optional.ofNullable(getMachineUserCrnIfApplicable(cluster)).orElse(cluster.getClusterPertain().getUserCrn());
                YarnScalingServiceV1Response yarnResponse = yarnMetricsClient.getYarnMetricsForCluster(cluster,
                        stackV4Response, policy.getHostGroup(), pollingUserCrn, Optional.of(mandatoryDownScaleCount));
                Map<String, String> hostFqdnsToInstanceId = stackResponseUtils.getCloudInstanceIdsForHostGroup(stackV4Response, policy.getHostGroup());

                int allowedDownscale = Math.min(mandatoryDownScaleCount, DEFAULT_MAX_SCALE_DOWN_STEP_SIZE);
                decommissionNodes = yarnResponseUtils.getYarnRecommendedDecommissionHostsForHostGroup(yarnResponse,
                        hostFqdnsToInstanceId).stream().limit(allowedDownscale).collect(Collectors.toList());
            }
            event.setDecommissionNodeIds(decommissionNodes);
        } catch (Exception ex) {
            LOGGER.error("Error retrieving decommission candidates for  policy '{}', adjustment type '{}', cluster '{}'",
                    policy.getName(), policy.getAdjustmentType(), cluster.getStackCrn(), ex);
            ScalingActivity activity = scalingActivityService.create(cluster, METRICS_COLLECTION_FAILED,
                    messagesService.getMessageWithArgs(AUTOSCALE_YARN_RECOMMENDATION_FAILED, ex), clock.getCurrentTimeMillis());
            scalingActivityService.setEndTime(activity.getId(), clock.getCurrentTimeMillis());
        }
    }

    private String getMachineUserCrnIfApplicable(Cluster cluster) {
        UpdateFailedDetails updateFailedDetails = cluster.getUpdateFailedDetails();
        if (updateFailedDetails == null || TimeUnit.MILLISECONDS.toMinutes(
                Instant.now().toEpochMilli() - updateFailedDetails.getLastExceptionTimestamp()) >= UPDATE_FAILED_INTERVAL_MINUTES) {
            if (cluster.getMachineUserCrn() != null) {
                LOGGER.debug("Attempting to invoke YARN with machineUser, forbiddenExceptionCount: {}, for evaluator: {}",
                        updateFailedDetails != null ? updateFailedDetails.getExceptionCount() : 0, EVALUATOR_NAME);
            }
            return cluster.getMachineUserCrn();
        }
        return null;
    }
}