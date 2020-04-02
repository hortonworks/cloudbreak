package com.sequenceiq.periscope.monitor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.MetricEvaluator;

@Component
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.metric-monitor", name = "enabled", havingValue = "true")
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
