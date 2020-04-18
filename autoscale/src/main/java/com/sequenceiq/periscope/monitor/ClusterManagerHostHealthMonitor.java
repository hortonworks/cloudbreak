package com.sequenceiq.periscope.monitor;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.ClusterManagerHostHealthEvaluator;

@Component("ClusterManagerHostHealthMonitor")
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.cluster-manager-host-health-monitor", name = "enabled", havingValue = "true")
public class ClusterManagerHostHealthMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "cluster-manager-host-health-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_FIVE_MIN_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster cluster) {
        return ClusterManagerHostHealthEvaluator.class;
    }

    @Override
    protected List<Cluster> getMonitored() {
        return getClusterService().findAllByStateAndNode(ClusterState.RUNNING, getPeriscopeNodeConfig().getId());
    }
}
