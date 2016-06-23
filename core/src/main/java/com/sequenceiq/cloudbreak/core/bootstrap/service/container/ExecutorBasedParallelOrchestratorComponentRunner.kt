package com.sequenceiq.cloudbreak.core.bootstrap.service.container

import java.util.concurrent.Callable
import java.util.concurrent.Future

import org.springframework.core.task.AsyncTaskExecutor

import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner

class ExecutorBasedParallelOrchestratorComponentRunner(private val asyncTaskExecutor: AsyncTaskExecutor) : ParallelOrchestratorComponentRunner {

    override fun submit(callable: Callable<Boolean>): Future<Boolean> {
        return asyncTaskExecutor.submit(callable)
    }
}
