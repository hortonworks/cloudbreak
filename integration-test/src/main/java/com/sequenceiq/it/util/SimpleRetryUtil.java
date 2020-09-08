package com.sequenceiq.it.util;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class SimpleRetryUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRetryUtil.class);

    private SimpleRetryUtil() {
    }

    public static void retry(int retryTimes, int retryWaitSeconds, Runnable action) {
        retry(retryTimes, retryWaitSeconds, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T retry(int retryTimes, int retryWaitSeconds, Supplier<T> action) {
        LOGGER.info("Trying action {} times, waiting {} seconds between.", retryTimes, retryWaitSeconds);

        T result = null;

        for (int timesTried = 1; timesTried <= retryTimes; timesTried++) {
            try {
                result = action.get();
                LOGGER.info("Action was successful on try {}.", timesTried);
                break;
            } catch (RuntimeException e) {
                if (timesTried < retryTimes) {
                    LOGGER.warn("Action failed on try {}, retrying after {} seconds.", timesTried, retryWaitSeconds, e);
                    try {
                        TimeUnit.SECONDS.sleep(retryWaitSeconds);
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    LOGGER.error("Failed to run command {} times.", retryTimes, e);
                    throw new TestFailException(String.format("Failed to run command %d times.", retryTimes), e);
                }
            }
        }

        return result;
    }

}
