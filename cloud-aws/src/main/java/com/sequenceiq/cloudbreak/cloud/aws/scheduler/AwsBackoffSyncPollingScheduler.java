package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class AwsBackoffSyncPollingScheduler<T> {

    private static final int POLLING_INTERVAL = 5;

    private static final int MAX_POLLING_INTERVAL = 50;

    private static final int MAX_POLLING_ATTEMPT = 1000;

    private static final int FAILURE_TOLERANT_ATTEMPT = 10;

    private static final String THROTTLING_ERROR_CODE = "Throttling";

    private static final Random RANDOM = ThreadLocalRandom.current();

    @Inject
    @Qualifier("cloudApiListeningScheduledExecutorService")
    private ListeningScheduledExecutorService scheduler;

    public T schedule(PollTask<T> task) throws ExecutionException, InterruptedException, TimeoutException {
        return schedule(task, POLLING_INTERVAL, MAX_POLLING_ATTEMPT, FAILURE_TOLERANT_ATTEMPT);
    }

    public T schedule(PollTask<T> task, int interval, int maxAttempt, int maxFailureTolerant) throws ExecutionException, InterruptedException, TimeoutException {
        T result;
        int actualFailureTolerant = 0;
        int multipliedInterval = interval;
        for (int i = 0; i < maxAttempt; i++) {
            if (task.cancelled()) {
                throw new CancellationException("Task was cancelled.");
            }
            try {
                ListenableScheduledFuture<T> ft = schedule(task, multipliedInterval);
                result = ft.get();
                multipliedInterval = interval;
                if (task.completed(result)) {
                    return result;
                }
            } catch (Exception ex) {
                if (ex.getMessage().contains(THROTTLING_ERROR_CODE)) {
                    multipliedInterval =
                            multipliedInterval >= MAX_POLLING_INTERVAL ? MAX_POLLING_INTERVAL : (multipliedInterval * 2) + RANDOM.nextInt(POLLING_INTERVAL);

                } else {
                    actualFailureTolerant++;
                    if (actualFailureTolerant >= maxFailureTolerant) {
                        throw ex;
                    }
                }
            }
        }
        throw new TimeoutException(String.format("Task (%s) did not finished within %d seconds", task.getClass().getSimpleName(), interval * maxAttempt));
    }

    public ListenableScheduledFuture<T> schedule(Callable<T> task, int interval) {
        return scheduler.schedule(task, interval, TimeUnit.SECONDS);
    }

}
