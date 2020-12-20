package com.sequenceiq.it.cloudbreak.util.wait;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.azure.AzureInstanceActionResult;

import rx.Completable;

public class CompletableWaitUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompletableWaitUtil.class);

    private final Completable action;

    private final long timeoutSeconds;

    private final Supplier<List<AzureInstanceActionResult>> check;

    public CompletableWaitUtil(Completable action, int timeoutValue, TimeUnit timeoutUnit, Supplier<List<AzureInstanceActionResult>> check) {
        this.action = action;
        this.timeoutSeconds = timeoutUnit.toSeconds(timeoutValue);
        this.check = check;
    }

    /**
     * Will check the completable regularly if it has finished. At the end a result check is performed.
     *
     * @throws Exception        any exception that occurs during the completable is working
     * @throws RuntimeException if after timeout of the completable returns the resultChecker fails
     */
    public void doWait() {
        int counter = 0;
        int timeoutOneCycleSeconds = 30;
        long numberOfCycles = timeoutSeconds / timeoutOneCycleSeconds + 1;
        boolean result = false;

        do {
            result = action.await(timeoutOneCycleSeconds, TimeUnit.SECONDS);
            counter++;
            LOGGER.info("Waiting on instance action, finished: {}", result);
        } while (!result && counter < numberOfCycles);
        Log.log("Azure instance action wait cycle returned with result {}", result);
        List<AzureInstanceActionResult> failedActions = check.get().stream().filter(r -> !r.isSuccess()).collect(Collectors.toList());
        if (!failedActions.isEmpty()) {
            throw new TestFailException(String.format("One or more instance actions have failed: %s",
                    failedActions.stream().map(this::getReadableAzureInstanceActionResult).collect(Collectors.joining(","))));
        }
        Log.log("Azure instance action completed successfully for all ids");
    }

    private String getReadableAzureInstanceActionResult(AzureInstanceActionResult azureInstanceActionResult) {
        return String.format("[id=%s, powerstate=%s]", azureInstanceActionResult.getInstanceId(), azureInstanceActionResult.getInstanceState());
    }
}
