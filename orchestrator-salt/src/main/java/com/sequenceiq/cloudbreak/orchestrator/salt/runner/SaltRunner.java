package com.sequenceiq.cloudbreak.orchestrator.salt.runner;

import java.util.concurrent.Callable;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Component
public class SaltRunner {

    private static final int SLEEP_TIME = 10000;

    @Value("${cb.max.salt.new.service.retry.onerror}")
    private int maxRetryOnError;

    @Value("${cb.max.salt.new.service.retry}")
    private int maxRetry;

    public Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return runner(bootstrap, exitCriteria, exitCriteriaModel, maxRetry, maxRetry);
    }

    public Callable<Boolean> runnerWithConfiguredErrorCount(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return runner(bootstrap, exitCriteria, exitCriteriaModel, maxRetry, maxRetryOnError);
    }

    public Callable<Boolean> runnerWithCalculatedErrorCount(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
            int maxRetry) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap(), maxRetry, SLEEP_TIME,
                calculateMaxRetryOnError(maxRetry));
    }

    public Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel, int maxRetry,
            int maxRetryOnError) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap(), maxRetry, SLEEP_TIME, maxRetryOnError);
    }

    public Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
            OrchestratorStateRetryParams orchestratorStateRetryParams) {
        int sleepTime = orchestratorStateRetryParams.getSleepTime() == -1 ? SLEEP_TIME : orchestratorStateRetryParams.getSleepTime();
        return runner(bootstrap, exitCriteria, exitCriteriaModel, orchestratorStateRetryParams.getMaxRetry(),
                orchestratorStateRetryParams.getMaxRetryOnError(), sleepTime, orchestratorStateRetryParams.getRetryPredicate());
    }

    private Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel, int maxRetry,
            int maxRetryOnError, int sleepTime) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap(), maxRetry, sleepTime, maxRetryOnError);
    }

    private Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel, int maxRetry,
            int maxRetryOnError, int sleepTime, java.util.function.Predicate<Exception> retryPredicate) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap(), maxRetry, sleepTime,
                maxRetryOnError, retryPredicate);
    }

    private int calculateMaxRetryOnError(int maxRetry) {
        return Math.min(maxRetry, maxRetryOnError);
    }
}
