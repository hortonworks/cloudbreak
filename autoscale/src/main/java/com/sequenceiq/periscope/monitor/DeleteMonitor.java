package com.sequenceiq.periscope.monitor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.ClusterDeleteEvaluator;

@Component
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.delete-monitor", name = "enabled", havingValue = "true")
public class DeleteMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "delete-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_TEN_MIN_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster monitored) {
        return ClusterDeleteEvaluator.class;
    }

    @Override
    protected List<Cluster> getMonitored() {
        return getClusterService()
                .findClusterIdsByStackTypeAndPeriscopeNodeId(StackType.WORKLOAD, getPeriscopeNodeConfig().getId())
                .stream().map(Cluster::new)
                .collect(Collectors.toList());
    }
}
