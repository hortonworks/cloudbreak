package com.sequenceiq.cloudbreak.cloud.scheduler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.sequenceiq.cloudbreak.cloud.task.FetchTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class SyncPollingScheduler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncPollingScheduler.class);

    @Inject
    private ListeningScheduledExecutorService scheduler;


    public T schedule(PollTask<T> task, int interval, int maxAttempt) throws ExecutionException, InterruptedException, TimeoutException {
        T result;
        for (int i = 0; i < maxAttempt; i++) {
            ListenableScheduledFuture<T> ft = schedule(task, interval);
            result = ft.get();
            if (task.completed(result)) {
                return result;
            }
        }
        throw new TimeoutException(String.format("Task did not finished within %d seconds", interval * maxAttempt));
    }

    public ListenableScheduledFuture<T> schedule(FetchTask<T> task, int interval) {
        return scheduler.schedule(task, interval, TimeUnit.SECONDS);
    }

}
