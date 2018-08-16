package com.sequenceiq.periscope.monitor.executor;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

@Service
public class ExecutorServiceWithRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorServiceWithRegistry.class);

    @Inject
    private EvaluatorExecutorRegistry evaluatorExecutorRegistry;

    @Inject
    private ExecutorService executorService;

    public void submitIfAbsent(EvaluatorExecutor evaluatorExecutor, long clusterId) {
        if (evaluatorExecutorRegistry.putIfAbsent(evaluatorExecutor, clusterId)) {
            try {
                executorService.submit(evaluatorExecutor);
            } finally {
                evaluatorExecutorRegistry.remove(evaluatorExecutor, clusterId);
            }
        } else {
            LOGGER.info("Evaluator {} is not accepted for cluster {}", evaluatorExecutor.getName(), clusterId);
        }
    }

    public void finished(EvaluatorExecutor evaluator, long clusterId) {
        evaluatorExecutorRegistry.remove(evaluator, clusterId);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
