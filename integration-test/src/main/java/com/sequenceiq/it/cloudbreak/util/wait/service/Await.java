package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.time.Duration;
import java.util.Map;

import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public interface Await<T, E extends Enum<E>> {

    default T await(T entity, E status, TestContext testContext, RunningParameter runningParameter, Duration pollingInterval, int maxRetry) {
        return entity;
    }

    default T await(T entity, Map<String, E> statuses, TestContext testContext, RunningParameter runningParameter, Duration pollingInterval, int maxRetry) {
        return entity;
    }
}
