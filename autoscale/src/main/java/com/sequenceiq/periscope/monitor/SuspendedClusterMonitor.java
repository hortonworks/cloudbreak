package com.sequenceiq.periscope.monitor;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.ClusterStateEvaluator;

@Component("SuspendedClusterMonitor")
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.suspended-cluster-monitor", name = "enabled", havingValue = "true")
public class SuspendedClusterMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "suspended-cluster-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_MIN_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster cluster) {
        return ClusterStateEvaluator.class;
    }

    @Override
    protected List<Cluster> getMonitored() {
        //To monitor Suspended clusters and purge them in periscope when they are deleted in CB.
        return getClusterService().findAllByStateAndNode(ClusterState.SUSPENDED, getPeriscopeNodeConfig().getId());
    }
}