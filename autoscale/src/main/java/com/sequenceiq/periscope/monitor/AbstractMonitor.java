package com.sequenceiq.periscope.monitor;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;
import com.sequenceiq.periscope.utils.LoggerUtils;

public abstract class AbstractMonitor implements Monitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMonitor.class);

    private PeriscopeNodeConfig periscopeNodeConfig;

    private ClusterService clusterService;

    private ApplicationContext applicationContext;

    private ExecutorService executorService;

    private LoggerUtils loggerUtils;

    @Override
    public void execute(JobExecutionContext context) {
        MDCBuilder.buildMdcContext();
        evalContext(context);
        for (Cluster cluster : getClusters()) {
            EvaluatorExecutor evaluatorExecutor = applicationContext.getBean(getEvaluatorType().getSimpleName(), EvaluatorExecutor.class);
            evaluatorExecutor.setContext(getContext(cluster));
            loggerUtils.logThreadPoolExecutorParameters(LOGGER, getEvaluatorType().getName(), executorService);
            executorService.submit(evaluatorExecutor);
            cluster.setLastEvaulated(System.currentTimeMillis());
            clusterService.save(cluster);
        }
    }

    void evalContext(JobExecutionContext context) {
        JobDataMap monitorContext = context.getJobDetail().getJobDataMap();
        applicationContext = (ApplicationContext) monitorContext.get(MonitorContext.APPLICATION_CONTEXT.name());
        executorService = applicationContext.getBean(ExecutorService.class);
        clusterService = applicationContext.getBean(ClusterService.class);
        periscopeNodeConfig = applicationContext.getBean(PeriscopeNodeConfig.class);
        loggerUtils = applicationContext.getBean(LoggerUtils.class);
    }

    PeriscopeNodeConfig getPeriscopeNodeConfig() {
        return periscopeNodeConfig;
    }

    ClusterService getClusterService() {
        return clusterService;
    }

    List<Cluster> getClusters() {
        return clusterService.findAllForNode(ClusterState.RUNNING, true, periscopeNodeConfig.getId());
    }
}
