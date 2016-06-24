package com.sequenceiq.periscope.monitor;

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.CronTimeEvaluator;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorContext;

@Component
public class TimeMonitor extends AbstractMonitor implements Monitor {

    @Override
    public String getIdentifier() {
        return "time-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.TIME_UPDATE_RATE_CRON;
    }

    @Override
    public Class getEvaluatorType() {
        return CronTimeEvaluator.class;
    }

    @Override
    public Map<String, Object> getContext(Cluster cluster) {
        return Collections.singletonMap(EvaluatorContext.CLUSTER_ID.name(), cluster.getId());
    }
}
