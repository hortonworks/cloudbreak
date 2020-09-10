package com.sequenceiq.it.util;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class SimpleRetryWrapper<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRetryWrapper.class);

    private final Supplier<T> action;

    private final String name;

    private final int retryTimes;

    private final int retryWaitSeconds;

    private SimpleRetryWrapper(Supplier<T> action, String name, int retryTimes, int retryWaitSeconds) {
        this.action = action;
        this.name = name;
        this.retryTimes = retryTimes;
        this.retryWaitSeconds = retryWaitSeconds;
    }

    private T run() {
        LOGGER.info("Trying [{}] action {} times, waiting {} seconds between.", name, retryTimes, retryWaitSeconds);

        T result = null;

        for (int timesTried = 1; timesTried <= retryTimes; timesTried++) {
            try {
                result = action.get();
                LOGGER.info("[{}] action was successful on try {}.", name, timesTried);
                break;
            } catch (RuntimeException e) {
                if (timesTried < retryTimes) {
                    LOGGER.warn("[{}] action failed on try {}, retrying after {} seconds.", name, timesTried, retryWaitSeconds, e);
                    try {
                        TimeUnit.SECONDS.sleep(retryWaitSeconds);
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    LOGGER.error("Failed to run [{}] action {} times.", name, retryTimes, e);
                    throw new TestFailException(String.format("Failed to run [%s] action %d times.", name, retryTimes), e);
                }
            }
        }

        return result;
    }

    public static SimpleRetryWrapperBuilder<Void> create(Runnable action) {
        return new SimpleRetryWrapperBuilder<>(() -> {
            action.run();
            return null;
        });
    }

    public static <T> SimpleRetryWrapperBuilder<T> create(Supplier<T> action) {
        return new SimpleRetryWrapperBuilder<>(action);
    }

    public static class SimpleRetryWrapperBuilder<T> {

        private Supplier<T> action;

        private String name;

        private int retryTimes = 5;

        private int retryWaitSeconds = 5;

        private SimpleRetryWrapperBuilder(Supplier<T> action) {
            this.action = action;
        }

        public SimpleRetryWrapperBuilder<T> withName(String name) {
            this.name = name;
            return this;
        }

        public SimpleRetryWrapperBuilder<T> withRetryTimes(int retryTimes) {
            this.retryTimes = retryTimes;
            return this;
        }

        public SimpleRetryWrapperBuilder<T> withRetryWaitSeconds(int retryWaitSeconds) {
            this.retryWaitSeconds = retryWaitSeconds;
            return this;
        }

        public T run() {
            Objects.requireNonNull(action, "Please provide the action to retry");
            Objects.requireNonNull(name, "Please provide a name for ease of debugging");

            return new SimpleRetryWrapper<>(action, name, retryTimes, retryWaitSeconds).run();
        }

    }
}
