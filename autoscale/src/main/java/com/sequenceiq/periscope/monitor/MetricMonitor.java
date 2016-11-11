package com.sequenceiq.periscope.monitor;

import java.util.Collections;
import java.util.Map;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.MetricEvaluator;

//@Component
public class MetricMonitor extends AbstractMonitor implements Monitor {

    @Override
    public String getIdentifier() {
        return "metric-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.METRIC_UPDATE_RATE_CRON;
    }

    @Override
    public Class getEvaluatorType() {
        return MetricEvaluator.class;
    }

    @Override
    public Map<String, Object> getContext(Cluster cluster) {
        return Collections.singletonMap(EvaluatorContext.CLUSTER_ID.name(), cluster.getId());
    }
}
