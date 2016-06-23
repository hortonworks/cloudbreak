package com.sequenceiq.cloudbreak.orchestrator.executor

import java.util.concurrent.Callable
import java.util.concurrent.Future

interface ParallelOrchestratorComponentRunner {

    fun submit(callable: Callable<Boolean>): Future<Boolean>
}
