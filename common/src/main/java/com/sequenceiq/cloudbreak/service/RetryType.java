package com.sequenceiq.cloudbreak.service;

import java.util.function.Supplier;

public enum RetryType {
    WITH_2_SEC_DELAY_MAX_5_TIMES(
            Retry::testWith2SecDelayMax5Times,
            Retry::testWith2SecDelayMax5Times
    ),
    WITH_2_SEC_DELAY_MAX_15_TIMES(
            Retry::testWith2SecDelayMax15Times
    ),
    WITH_1_SEC_DELAY_MAX_5_TIMES(
            Retry::testWith1SecDelayMax5Times
    ),
    WITH_1_SEC_DELAY_MAX_3_TIMES(
            Retry::testWith1SecDelayMax3Times
    ),
    WITH_1_SEC_DELAY_MAX_5_TIMES_MAX_DELAY_5_MINUTES_MULTIPLIER_5(
            Retry::testWith1SecDelayMax5TimesMaxDelay5MinutesMultiplier5
    ),
    NO_RETRY(
            Retry::testWithoutRetry
    );

    private final RetrySupplierExecutor supplierExecutor;

    private final RetryRunnableExecutor runnableExecutor;

    RetryType(RetrySupplierExecutor supplierExecutor) {
        this(supplierExecutor, null);
    }

    RetryType(RetrySupplierExecutor supplierExecutor, RetryRunnableExecutor runnableExecutor) {
        this.supplierExecutor = supplierExecutor;
        this.runnableExecutor = runnableExecutor;
    }

    public <T> T execute(Retry retry, Supplier<T> action) {
        return supplierExecutor.execute(retry, action);
    }

    public void execute(Retry retry, Runnable action) {
        if (runnableExecutor != null) {
            runnableExecutor.execute(retry, action);
        } else {
            // Fallback: direct run if runnable executor not defined for this type
            action.run();
        }
    }

    @FunctionalInterface
    private interface RetrySupplierExecutor {
        <T> T execute(Retry retry, Supplier<T> action);
    }

    @FunctionalInterface
    private interface RetryRunnableExecutor {
        void execute(Retry retry, Runnable action);
    }
}
