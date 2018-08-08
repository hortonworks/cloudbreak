package com.sequenceiq.periscope.monitor;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.PrometheusEvaluator;

@Component
public class PrometheusMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "prometheus-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.PROMETHEUS_UPDATE_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster cluster) {
        return PrometheusEvaluator.class;
    }
}
