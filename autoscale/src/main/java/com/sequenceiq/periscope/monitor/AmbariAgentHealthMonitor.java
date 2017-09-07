package com.sequenceiq.periscope.monitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.AmbariAgentHealthEvaluator;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorContext;

@Component
public class AmbariAgentHealthMonitor extends AbstractMonitor {

    @Override
    public String getIdentifier() {
        return "ambari-agent-health-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_MIN_RATE_CRON;
    }

    @Override
    public Class getEvaluatorType() {
        return AmbariAgentHealthEvaluator.class;
    }

    @Override
    public Map<String, Object> getContext(Cluster cluster) {
        return Collections.singletonMap(EvaluatorContext.CLUSTER_ID.name(), cluster.getId());
    }

    @Override
    List<Cluster> getClusters() {
        return getClusterService().findAll(ClusterState.RUNNING);
    }
}
