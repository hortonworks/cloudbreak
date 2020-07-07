package com.sequenceiq.periscope.monitor;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.monitor.evaluator.load.YarnScalingMetricsCollector;

@Component
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.load-monitor", name = "enabled", havingValue = "true")
public class LoadScalingMetricsMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "load-scaling-metrics-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_30_SECS_RATE_CRON;
    }

    @Override
    public Class<? extends EvaluatorExecutor> getEvaluatorType(Cluster cluster) {
        return YarnScalingMetricsCollector.class;
    }

    @Override
    protected List<Cluster> getMonitored() {
        List<Long> clusterIds = getClusterService().findLoadAlertClustersForNode(StackType.WORKLOAD,
                ClusterState.RUNNING, true, getPeriscopeNodeConfig().getId());
        return clusterIds.isEmpty() ? List.of() : getClusterService().findClustersByClusterIds(clusterIds);
    }
}
