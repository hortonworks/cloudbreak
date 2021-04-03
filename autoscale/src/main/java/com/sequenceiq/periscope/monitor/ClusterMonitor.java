package com.sequenceiq.periscope.monitor;

import java.util.List;

import org.quartz.JobExecutionContext;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;

public abstract class ClusterMonitor extends AbstractMonitor<Cluster> {

    private PeriscopeNodeConfig periscopeNodeConfig;

    private ClusterService clusterService;

    @Override
    protected List<Cluster> getMonitored() {
        return clusterService.findAllForNode(ClusterState.RUNNING, true, periscopeNodeConfig.getId());
    }

    @Override
    void evalContext(JobExecutionContext context) {
        super.evalContext(context);
        clusterService = getApplicationContext().getBean(ClusterService.class);
        periscopeNodeConfig = getApplicationContext().getBean(PeriscopeNodeConfig.class);
    }

    @Override
    protected void updateLastEvaluated(Cluster monitored) {
        clusterService.updateLastEvaluated(monitored);
    }

    PeriscopeNodeConfig getPeriscopeNodeConfig() {
        return periscopeNodeConfig;
    }

    ClusterService getClusterService() {
        return clusterService;
    }

    protected EvaluatorExecutor getEvaluatorExecutorBean(Cluster cluster) {
        return getApplicationContext().getBean(getEvaluatorType(cluster).getSimpleName(), EvaluatorExecutor.class);
    }

    @Override
    public EvaluatorContext getContext(Cluster cluster) {
        return new ClusterIdEvaluatorContext(cluster.getId());
    }
}
