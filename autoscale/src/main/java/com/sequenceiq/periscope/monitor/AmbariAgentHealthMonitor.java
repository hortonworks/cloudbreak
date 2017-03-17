package com.sequenceiq.periscope.monitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.AmbariAgentHealthEvaluator;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

@Component
public class AmbariAgentHealthMonitor extends AbstractMonitor implements Monitor {

    @Override
    public String getIdentifier() {
        return "ambari-agent-health-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_MIN_RATE_CRON;
    }

    @Override
    public Class getEvaluatorType() {
        return AmbariAgentHealthEvaluator.class;
    }

    @Override
    public Map<String, Object> getContext(Cluster cluster) {
        return Collections.singletonMap(EvaluatorContext.CLUSTER_ID.name(), cluster.getId());
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        evalContext(context);
        List<Cluster> clustersToRecover = getClusterService().findAll(ClusterState.RUNNING);
        clustersToRecover.addAll(getClusterService().findAll(ClusterState.DISABLED));
        for (Cluster cluster : clustersToRecover) {
            EvaluatorExecutor evaluatorExecutor = getApplicationContext().getBean(getEvaluatorType().getSimpleName(), EvaluatorExecutor.class);
            evaluatorExecutor.setContext(getContext(cluster));
            getExecutorService().submit(evaluatorExecutor);
        }
    }
}
