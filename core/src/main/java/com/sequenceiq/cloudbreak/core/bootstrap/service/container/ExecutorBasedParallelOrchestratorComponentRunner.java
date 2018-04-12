package com.sequenceiq.cloudbreak.core.bootstrap.service.container;

import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ExecutorBasedParallelOrchestratorComponentRunner implements ParallelOrchestratorComponentRunner {

    private final AsyncTaskExecutor asyncTaskExecutor;

    public ExecutorBasedParallelOrchestratorComponentRunner(AsyncTaskExecutor asyncTaskExecutor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    @Override
    public Future<Boolean> submit(Callable<Boolean> callable) {
        return asyncTaskExecutor.submit(callable);
    }
}
