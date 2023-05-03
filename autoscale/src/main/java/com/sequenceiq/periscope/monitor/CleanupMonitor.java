package com.sequenceiq.periscope.monitor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.model.ScalingActivities;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.monitor.evaluator.cleanup.ActivityCleanupEvaluator;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeService;

@Component
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.cleanup-monitor", name = "enabled", havingValue = "true")
public class CleanupMonitor extends ScalingActivityMonitor {

    private final AtomicLong scalingActivitiesIdGenerator = new AtomicLong(0L);

    @Override
    protected List<ScalingActivities> getMonitored() {
        Set<ActivityStatus> statuses = ActivityStatus.getStatusesApplicableForCleanup();
        PeriscopeNodeService periscopeNodeService = getPeriscopeNodeService();
        if (periscopeNodeService.isLeader(getPeriscopeNodeConfig().getId())) {
            Set<Long> activityIdsToCleanup = getScalingActivityService().findAllInStatusesThatStartedBefore(statuses,
                    getCleanupConfig().getCleanupDurationHours());
            return activityIdsToCleanup.isEmpty() ? emptyList() :
                    singletonList(new ScalingActivities(scalingActivitiesIdGenerator.incrementAndGet(), activityIdsToCleanup));
        }
        return emptyList();
    }

    @Override
    public String getIdentifier() {
        return "scaling-activity-cleanup-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_TEN_MIN_RATE_CRON;
    }

    @Override
    public Class<? extends EvaluatorExecutor> getEvaluatorType(ScalingActivities monitored) {
        return ActivityCleanupEvaluator.class;
    }
}
