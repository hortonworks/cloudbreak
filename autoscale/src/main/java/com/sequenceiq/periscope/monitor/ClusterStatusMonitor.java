package com.sequenceiq.periscope.monitor;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.ClusterStateEvaluator;

@Component("ClusterStatusMonitor")
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.cluster-status-monitor", name = "enabled", havingValue = "true")
public class ClusterStatusMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "cluster-status-monitor";
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