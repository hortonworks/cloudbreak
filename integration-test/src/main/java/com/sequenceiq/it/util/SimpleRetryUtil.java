package com.sequenceiq.it.util;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SimpleRetryUtil {

    private SimpleRetryUtil() {
    }

    public static void retry(int retryTimes, int retryWaitSeconds, Runnable action) {
        retry(retryTimes, retryWaitSeconds, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T retry(int retryTimes, int retryWaitSeconds, Supplier<T> action) {
        T result = null;

        int timesTried = 0;
        RuntimeException exception;
        do {
            timesTried++;
            exception = null;
            try {
                result = action.get();
            } catch (RuntimeException e) {
                exception = e;
                try {
                    TimeUnit.SECONDS.sleep(retryWaitSeconds);
                } catch (InterruptedException ignored) {
                }
            }
        } while (exception != null && timesTried < retryTimes);

        if (Objects.isNull(exception)) {
            return result;
        }

        throw new RuntimeException(String.format("Failed to run command %d times.", retryTimes), exception);
    }

}
