package com.sequenceiq.periscope.monitor;

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.MetricEvaluator;
import com.sequenceiq.periscope.monitor.evaluator.PrometheusEvaluator;

@Component
public class PrometheusMonitor extends AbstractMonitor implements Monitor {

    @Override
    public String getIdentifier() {
        return "prometheus-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.PROMETHEUS_UPDATE_RATE_CRON;
    }

    @Override
    public Class getEvaluatorType() {
        return PrometheusEvaluator.class;
    }

    @Override
    public Map<String, Object> getContext(Cluster cluster) {
        return Collections.singletonMap(EvaluatorContext.CLUSTER_ID.name(), cluster.getId());
    }
}
