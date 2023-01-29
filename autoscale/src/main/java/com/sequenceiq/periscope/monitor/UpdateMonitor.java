package com.sequenceiq.periscope.monitor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.ScalingActivities;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.monitor.evaluator.update.ActivityUpdateEvaluator;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeService;

@Component
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.update-monitor", name = "enabled", havingValue = "true")
public class UpdateMonitor extends ScalingActivityMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateMonitor.class);

    private static final Integer ACTIVITY_IDS_LIMIT = 200;

    private final AtomicLong scalingActivitiesIdGenerator = new AtomicLong(0L);

    @Override
    protected List<ScalingActivities> getMonitored() {
        PeriscopeNodeService periscopeNodeService = getPeriscopeNodeService();
        String periscopeNodeId = getPeriscopeNodeConfig().getId();
        if (periscopeNodeService.isLeader(periscopeNodeId)) {
            LOGGER.info("Fetching monitoredData via periscopeNode: {} for scaling-activity-update-monitor", periscopeNodeId);
            Set<Long> scalingActivityIds = getScalingActivityService().findAllIdsOfSuccessfulAndInProgressScalingActivity();
            Set<Long> limitedActivityIds = scalingActivityIds.stream().limit(ACTIVITY_IDS_LIMIT).collect(toSet());
            if (limitedActivityIds.isEmpty()) {
                return emptyList();
            }
            LOGGER.info("Limited monitored data of activity ids with a backlog of {} ids", scalingActivityIds.size() - limitedActivityIds.size());
            return singletonList(new ScalingActivities(scalingActivitiesIdGenerator.incrementAndGet(),
                    limitedActivityIds));
        }
        return emptyList();
    }

    @Override
    public String getIdentifier() {
        return "scaling-activity-update-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_TWO_MIN_RATE_CRON;
    }

    @Override
    public Class<? extends EvaluatorExecutor> getEvaluatorType(ScalingActivities monitored) {
        return ActivityUpdateEvaluator.class;
    }
}
