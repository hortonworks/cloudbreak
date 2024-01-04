package com.sequenceiq.periscope.monitor.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

@Service
public class ExecutorServiceWithRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorServiceWithRegistry.class);

    @Inject
    private EvaluatorExecutorRegistry evaluatorExecutorRegistry;

    @Inject
    @Qualifier("periscopeListeningScheduledExecutorService")
    private ExecutorService executorService;

    public void submitIfAbsent(EvaluatorExecutor evaluatorExecutor, long resourceId) {
        if (evaluatorExecutorRegistry.putIfAbsent(evaluatorExecutor, resourceId)) {
            try {
                executorService.submit(evaluatorExecutor);
            } catch (RejectedExecutionException e) {
                evaluatorExecutorRegistry.remove(evaluatorExecutor, resourceId);
                throw e;
            }
        } else {
            LOGGER.info("Evaluator {} is not accepted for resource {}", evaluatorExecutor.getName(), resourceId);
        }
    }

    public void finished(EvaluatorExecutor evaluator, long clusterId) {
        evaluatorExecutorRegistry.remove(evaluator, clusterId);
    }

    public int activeCount() {
        return evaluatorExecutorRegistry.activeCount();
    }
}
