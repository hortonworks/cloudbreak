package com.sequenceiq.periscope.monitor;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.CronTimeEvaluator;

@Component
public class TimeMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "time-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.TIME_UPDATE_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster cluster) {
        return CronTimeEvaluator.class;
    }

}
