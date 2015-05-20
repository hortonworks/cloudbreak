package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.orchestrator.ParallelContainerRunner;

public class ExecutorBasedParallelContainerRunner implements ParallelContainerRunner {

    private AsyncTaskExecutor asyncTaskExecutor;

    public ExecutorBasedParallelContainerRunner(AsyncTaskExecutor asyncTaskExecutor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    @Override
    public Future<Boolean> submit(Callable<Boolean> callable) {
        return asyncTaskExecutor.submit(callable);
    }
}
