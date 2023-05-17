package com.sequenceiq.periscope.monitor.evaluator.cleanup;

import static com.sequenceiq.periscope.domain.MetricType.SCALING_ACTIVITY_CLEANUP_CANDIDATES;
import static com.sequenceiq.periscope.domain.MetricType.SCALING_ACTIVITY_LAST_CLEANUP_INVOCATION;
import static com.sequenceiq.periscope.domain.MetricType.STALE_SCALING_ACTIVITY;
import static com.sequenceiq.periscope.domain.MetricType.TOTAL_SCALING_ACTIVITIES;

import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.model.ScalingActivities;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.context.ScalingActivitiesEvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.service.PeriscopeMetricService;
import com.sequenceiq.periscope.service.ScalingActivityService;

@Component("ActivityCleanupEvaluator")
@Scope("prototype")
public class ActivityCleanupEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityCleanupEvaluator.class);

    private static final String EVALUATOR_NAME = ActivityCleanupEvaluator.class.getName();

    @Inject
    private ScalingActivityService scalingActivityService;

    @Inject
    private PeriscopeMetricService metricService;

    private ScalingActivities scalingActivities;

    @Nonnull
    @Override
    public EvaluatorContext getContext() {
        return new ScalingActivitiesEvaluatorContext(scalingActivities);
    }

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    @Override
    public void setContext(EvaluatorContext context) {
        this.scalingActivities = (ScalingActivities) context.getData();
    }

    @Override
    protected void execute() {
        if (Objects.isNull(scalingActivities)) {
            LOGGER.info("No scalingActivities found");
            return;
        }

        LOGGER.info("Executing ScalingActivityCleanupEvaluator: {}", scalingActivities.getId());

        try {
            if (scalingActivities.getActivityIds().isEmpty()) {
                LOGGER.info("No activityIds present for ScalingActivityCleanup, skipping request to purge scalingActivity");
            } else {
                LOGGER.info("Purging {} scalingActivity Ids as part of cleanup", scalingActivities.getActivityIds().size());
                scalingActivityService.deleteScalingActivityByIds(scalingActivities.getActivityIds());
            }
            calculateUnprocessedScalingActivityMetrics(scalingActivities.getActivityIds().size());
            caclulateLastCleanupInvocation(scalingActivities);
        } catch (Exception e) {
            LOGGER.error("Exception occurred when executing ScalingActivityCleanupEvaluator: {}", scalingActivities.getId(), e);
        } finally {
            LOGGER.info("Finished executing ScalingActivityCleanupEvaluator: {}", scalingActivities.getId());
        }
    }

    private void caclulateLastCleanupInvocation(ScalingActivities activties) {
        LOGGER.info("Submitting last invocation of activity cleanup for activity count: {}", activties.getActivityIds().size());
        metricService.gauge(SCALING_ACTIVITY_LAST_CLEANUP_INVOCATION, scalingActivities.getLastEvaluated());
    }

    private void calculateUnprocessedScalingActivityMetrics(long totalCleanupCandidates) {
        LOGGER.info("Submitting untracked scaling activity metrics");
        Set<ActivityStatus> applicableStatuses = Sets.difference(Set.of(ActivityStatus.values()), ActivityStatus.getStatusesApplicableForCleanup());
        metricService.gauge(STALE_SCALING_ACTIVITY, scalingActivityService.countAllActivitiesInStatuses(applicableStatuses));
        metricService.gauge(TOTAL_SCALING_ACTIVITIES, scalingActivityService.countAllActivities());
        metricService.gauge(SCALING_ACTIVITY_CLEANUP_CANDIDATES, totalCleanupCandidates);
    }
}
