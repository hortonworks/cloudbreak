package com.sequenceiq.it.cloudbreak.util.wait;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Completable;

public class CompletableWaitUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompletableWaitUtil.class);

    private final Completable action;

    private final long timeoutSeconds;

    private final Supplier<Boolean> check;

    public CompletableWaitUtil(Completable action, int timeoutValue, TimeUnit timeoutUnit, Supplier<Boolean> check) {
        this.action = action;
        this.timeoutSeconds = timeoutUnit.toSeconds(timeoutValue);
        this.check = check;
    }

    /**
     * Will check the completable regularly if it has finished. At the end a result check is performed.
     *
     * @throws Exception:        any exception that occurs during the completable is working
     * @throws RuntimeException: if after timeout of the completable returns the resultChecker fails
     */
    public void doWait() {
        int counter = 0;
        int timeoutOneCycleSeconds = 30;
        long numberOfCycles = timeoutSeconds / timeoutOneCycleSeconds + 1;
        boolean result = false;

        do {
            result = action.await(timeoutOneCycleSeconds, TimeUnit.SECONDS);
            counter++;
            LOGGER.debug("Waiting on instance action, finished: {}", result);
        } while (!result && counter < numberOfCycles);
        LOGGER.debug("Wait cycle returned with result {}", result);
        if (!check.get()) {
            throw new RuntimeException("Stopping of instances has timed out");
        }
    }
}
