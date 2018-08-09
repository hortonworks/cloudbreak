package com.sequenceiq.periscope.monitor.executor;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

@Service
public class ExecutorServiceWithRegistry {

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
        }
    }

    public void finished(EvaluatorExecutor evaluator, long clusterId) {
        evaluatorExecutorRegistry.remove(evaluator, clusterId);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
