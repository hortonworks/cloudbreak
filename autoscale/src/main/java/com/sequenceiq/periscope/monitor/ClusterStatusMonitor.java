package com.sequenceiq.periscope.monitor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
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
        return MonitorUpdateRate.CLUSTER_MONITOR_EVERY_MIN_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster cluster) {
        return ClusterStateEvaluator.class;
    }

    @Override
    protected List<Cluster> getMonitored() {
        return getClusterService().findClusterIdsByStackTypeAndPeriscopeNodeId(StackType.WORKLOAD, getPeriscopeNodeConfig().getId())
                .stream().map(clusterId -> new Cluster(clusterId))
                .collect(Collectors.toList());
    }
}