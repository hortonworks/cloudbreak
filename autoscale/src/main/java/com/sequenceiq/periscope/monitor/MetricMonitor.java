package com.sequenceiq.periscope.monitor;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.MetricEvaluator;

@Component
public class MetricMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "metric-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.METRIC_UPDATE_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster cluster) {
        return MetricEvaluator.class;
    }
}
