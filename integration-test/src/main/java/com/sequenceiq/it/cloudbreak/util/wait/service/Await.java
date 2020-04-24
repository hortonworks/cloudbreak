package com.sequenceiq.it.cloudbreak.util.wait.service;

import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public interface Await<T, E extends Enum<E>> {

    T await(T entity, E status, TestContext testContext, RunningParameter runningParameter, long pollingInterval, int maxRetry);
}
