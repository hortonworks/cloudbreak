package com.sequenceiq.periscope.monitor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.ClusterScaleStatusEvaluator;

@Component("ClusterScaleStatusMonitor")
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.cluster-scale-status-monitor", name = "enabled", havingValue = "true")
public class ClusterScaleStatusMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "cluster-scale-status-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_FIVE_MIN_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster cluster) {
        return ClusterScaleStatusEvaluator.class;
    }

    @Override
    protected List<Cluster> getMonitored() {
        return getClusterService().findLoadAlertClusterIdsForPeriscopeNodeId(StackType.WORKLOAD, ClusterState.SUSPENDED, true, getPeriscopeNodeConfig().getId())
                .stream().map(Cluster::new)
                .collect(Collectors.toList());
    }
}