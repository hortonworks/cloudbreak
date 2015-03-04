package com.sequenceiq.periscope.monitor;

import java.util.concurrent.ExecutorService;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.request.Request;
import com.sequenceiq.periscope.registry.ClusterRegistry;

public abstract class AbstractMonitor implements Monitor {

    private ClusterRegistry clusterRegistry;
    private ApplicationContext applicationContext;
    private ExecutorService executorService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        evalContext(context);
        for (Cluster cluster : clusterRegistry.getAll()) {
            if (cluster.isRunning()) {
                Request request = applicationContext.getBean(getRequestType().getSimpleName(), Request.class);
                request.setContext(getRequestContext(cluster));
                executorService.submit(request);
            }
        }
    }

    private void evalContext(JobExecutionContext context) {
        JobDataMap monitorContext = context.getJobDetail().getJobDataMap();
        applicationContext = (ApplicationContext) monitorContext.get(MonitorContext.APPLICATION_CONTEXT.name());
        executorService = applicationContext.getBean(ExecutorService.class);
        clusterRegistry = applicationContext.getBean(ClusterRegistry.class);
    }

}
