package com.sequenceiq.periscope.monitor;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.service.RejectedThreadService;

public abstract class AbstractMonitor<M extends Monitored> implements Monitor<M> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMonitor.class);

    private ApplicationContext applicationContext;

    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    private RejectedThreadService rejectedThreadService;

    @Override
    public void execute(JobExecutionContext context) {
        MDCBuilder.buildMdcContext();
        evalContext(context);
        List<M> monitoredData = getMonitored();
        LOGGER.debug("Job started: {}, monitored: {}", context.getJobDetail().getKey(), monitoredData.size());
        for (M monitored : monitoredData) {
            try {
                EvaluatorExecutor evaluatorExecutor = getEvaluatorExecutorBean(monitored);
                EvaluatorContext evaluatorContext = getContext(monitored);
                evaluatorExecutor.setContext(evaluatorContext);
                executorServiceWithRegistry.submitIfAbsent(evaluatorExecutor, evaluatorContext.getItemId());
                LOGGER.debug("Successfully submitted {} for cluster {}.", evaluatorExecutor.getName(), evaluatorContext.getData());
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
        executorServiceWithRegistry = applicationContext.getBean(ExecutorServiceWithRegistry.class);
        rejectedThreadService = applicationContext.getBean(RejectedThreadService.class);
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    protected EvaluatorExecutor getEvaluatorExecutorBean(M monitored) {
        return applicationContext.getBean(getEvaluatorType(monitored).getSimpleName(), EvaluatorExecutor.class);
    }

    protected abstract List<M> getMonitored();

    protected abstract M save(M monitored);

    protected RejectedThreadService getRejectedThreadService() {
        return rejectedThreadService;
    }
}
