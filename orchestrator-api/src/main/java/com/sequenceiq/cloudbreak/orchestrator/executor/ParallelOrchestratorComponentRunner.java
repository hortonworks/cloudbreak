package com.sequenceiq.cloudbreak.orchestrator.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface ParallelOrchestratorComponentRunner {

    Future<Boolean> submit(Callable<Boolean> callable);
}
