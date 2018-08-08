package com.sequenceiq.periscope.monitor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;
import com.sequenceiq.periscope.utils.LoggerUtils;
import com.sequenceiq.periscope.service.RejectedThreadService;

public abstract class AbstractMonitor<M extends Monitored> implements Monitor<M> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMonitor.class);

    private PeriscopeNodeConfig periscopeNodeConfig;

    private ClusterService clusterService;

    private ApplicationContext applicationContext;

    private ExecutorService executorService;

    private LoggerUtils loggerUtils;

    private RejectedThreadService rejectedThreadService;

    @Override
    public void execute(JobExecutionContext context) {
        MDCBuilder.buildMdcContext();
        evalContext(context);
        List<M> monitoredData = getMonitored();
        LOGGER.info("Job started: {}, monitored: {}", context.getJobDetail().getKey(), monitoredData.size());
        for (M monitored : monitoredData) {
            try {
                loggerUtils.logThreadPoolExecutorParameters(LOGGER, getEvaluatorType(monitored).getName(), executorService);
                EvaluatorExecutor evaluatorExecutor = getEvaluatorExecutorBean(monitored);
                EvaluatorContext evaluatorContext = getContext(monitored);
                evaluatorExecutor.setContext(evaluatorContext);
                executorService.submit(evaluatorExecutor);
                LOGGER.info("Succesfully submitted {}.", evaluatorContext.getData());
                rejectedThreadService.remove(evaluatorContext.getData());
                monitored.setLastEvaluated(System.currentTimeMillis());
                save(monitored);
            } catch (RejectedExecutionException ignore) {

            }
        }
    }

    void evalContext(JobExecutionContext context) {
        JobDataMap monitorContext = context.getJobDetail().getJobDataMap();
        applicationContext = (ApplicationContext) monitorContext.get(MonitorContext.APPLICATION_CONTEXT.name());
        executorService = applicationContext.getBean(ExecutorService.class);
        clusterService = applicationContext.getBean(ClusterService.class);
        periscopeNodeConfig = applicationContext.getBean(PeriscopeNodeConfig.class);
        loggerUtils = applicationContext.getBean(LoggerUtils.class);
        rejectedThreadService = applicationContext.getBean(RejectedThreadService.class);
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    protected EvaluatorExecutor getEvaluatorExecutorBean(M cluster) {
        return applicationContext.getBean(getEvaluatorType(cluster).getSimpleName(), EvaluatorExecutor.class);
    }

    protected abstract List<M> getMonitored();

    protected abstract M save(M monitored);

    protected RejectedThreadService getRejectedThreadService() {
        return rejectedThreadService;
    }
}
