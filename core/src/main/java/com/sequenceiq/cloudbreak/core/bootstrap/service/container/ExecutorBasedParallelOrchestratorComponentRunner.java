package com.sequenceiq.cloudbreak.core.bootstrap.service.container;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;

public class ExecutorBasedParallelOrchestratorComponentRunner implements ParallelOrchestratorComponentRunner {

    private AsyncTaskExecutor asyncTaskExecutor;

    public ExecutorBasedParallelOrchestratorComponentRunner(AsyncTaskExecutor asyncTaskExecutor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    @Override
    public Future<Boolean> submit(Callable<Boolean> callable) {
        return asyncTaskExecutor.submit(callable);
    }
}
