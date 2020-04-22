package com.sequenceiq.cloudbreak.cloud.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class SyncPollingScheduler<T> {

    private static final int POLLING_INTERVAL = 1000;

    private static final int MAX_POLLING_ATTEMPT = 5000;

    private static final int FAILURE_TOLERANT_ATTEMPT = 3;

    public T schedule(PollTask<T> task) throws Exception {
        return schedule(task, POLLING_INTERVAL, MAX_POLLING_ATTEMPT, FAILURE_TOLERANT_ATTEMPT);
    }

    public T schedule(PollTask<T> task, int interval, int maxAttempt, int maxFailureTolerant) throws Exception {
        AtomicInteger actualFailureTolerant = new AtomicInteger(0);

        try {
            return Polling.stopAfterAttempt(maxAttempt)
                    .waitPeriodly(interval, TimeUnit.MILLISECONDS)
                    .stopIfException(true)
                    .run(() -> {
                        if (task.cancelled()) {
                            return AttemptResults.breakFor(new CancellationException("Task was cancelled."));
                        }
                        try {
                            T callResult = task.call();
                            if (task.completed(callResult)) {
                                return AttemptResults.finishWith(callResult);
                            }
                        } catch (Exception ex) {
                            int currentTolerant = actualFailureTolerant.incrementAndGet();
                            if (currentTolerant >= maxFailureTolerant) {
                                return AttemptResults.breakFor(ex);
                            }
                        }
                        return AttemptResults.justContinue();
                    });
        } catch (PollerStoppedException e) {
            throw new TimeoutException(String.format("Task (%s) did not finish within %d seconds", task.getClass().getSimpleName(), interval * maxAttempt));
        } catch (UserBreakException e) {
            throw (Exception) e.getCause();
        }
    }
}
