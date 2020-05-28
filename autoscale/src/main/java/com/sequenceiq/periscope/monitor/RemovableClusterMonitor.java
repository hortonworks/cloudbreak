package com.sequenceiq.periscope.monitor;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.ClusterStateEvaluator;

@Component("RemovableClusterMonitor")
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.removable-cluster-monitor", name = "enabled", havingValue = "true")
public class RemovableClusterMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "removable-cluster-monitor";
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
        return getClusterService().findAllByPeriscopeNodeId(getPeriscopeNodeConfig().getId());
    }
}