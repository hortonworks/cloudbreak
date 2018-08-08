package com.sequenceiq.periscope.monitor;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.AmbariAgentHealthEvaluator;

@Component
public class AmbariAgentHealthMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "ambari-agent-health-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_MIN_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster cluster) {
        return AmbariAgentHealthEvaluator.class;
    }

    @Override
    protected List<Cluster> getMonitored() {
        return getClusterService().findAllByStateAndNode(ClusterState.RUNNING, getPeriscopeNodeConfig().getId());
    }
}
