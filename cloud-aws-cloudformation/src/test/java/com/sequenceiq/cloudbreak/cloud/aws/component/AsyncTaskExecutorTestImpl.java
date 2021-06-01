package com.sequenceiq.cloudbreak.cloud.aws.component;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;

public class AsyncTaskExecutorTestImpl implements AsyncTaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncTaskExecutorTestImpl.class);

    @Override
    public void execute(Runnable task, long startTimeout) {
        LOGGER.info("execute called");
        task.run();
    }

    @Override
    public Future<?> submit(Runnable task) {
        LOGGER.info("submit called");
        task.run();
        return new FutureTestImpl<>(null);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        LOGGER.info("submit called");
        return new FutureTestImpl<>(getResult(task));
    }

    @Override
    public void execute(Runnable task) {
        LOGGER.info("execute called");
        task.run();
    }

    <T> T getResult(Callable<T> task) {
        try {
            return task.call();
        } catch (Exception e) {
            LOGGER.error("Error completing call to callable", e);
            throw new RuntimeException("Error in test: calling callable.call returned exception", e);
        }
    }
}
