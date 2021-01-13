package com.sequenceiq.periscope.monitor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.CronTimeEvaluator;

@Component
@ConditionalOnProperty(prefix = "periscope.enabledAutoscaleMonitors.time-monitor", name = "enabled", havingValue = "true")
public class TimeMonitor extends ClusterMonitor {

    @Override
    public String getIdentifier() {
        return "time-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.TIME_UPDATE_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType(Cluster cluster) {
        return CronTimeEvaluator.class;
    }

    @Override
    protected List<Cluster> getMonitored() {
        return getClusterService().findTimeAlertClusterIdsForPeriscopeNodeId(StackType.WORKLOAD, true, getPeriscopeNodeConfig().getId())
                .stream().map(clusterId -> new Cluster(clusterId))
                .collect(Collectors.toList());
    }
}
