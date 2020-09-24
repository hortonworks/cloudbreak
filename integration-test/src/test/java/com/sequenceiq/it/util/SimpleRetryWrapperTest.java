package com.sequenceiq.it.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleRetryWrapperTest {

    private static final String NAME = "test";

    private static final int RETRY_WAIT_SECONDS = 0;

    private static final int RETRY_TIMES = 5;

    @Test
    void shouldSucceed() {
        int expectedResult = 5;
        AtomicInteger tries = new AtomicInteger();

        Integer result = SimpleRetryWrapper.create(() -> {
            tries.incrementAndGet();
            return expectedResult;
        }).withName(NAME).withRetryTimes(RETRY_TIMES).withRetryWaitSeconds(RETRY_WAIT_SECONDS).run();

        Assertions.assertThat(result).isEqualTo(expectedResult);
        Assertions.assertThat(tries.get()).isEqualTo(1);
    }

    @Test
    void shouldRetry() {
        AtomicInteger tries = new AtomicInteger();
        RuntimeException exception = new RuntimeException("failure");

        Assertions.assertThatThrownBy(() -> SimpleRetryWrapper.create(() -> {
            tries.incrementAndGet();
            throw exception;
        }).withName(NAME).withRetryTimes(RETRY_TIMES).withRetryWaitSeconds(RETRY_WAIT_SECONDS).run())
                .hasMessage("Failed to run [test] action 5 times.")
                .hasCause(exception);

        Assertions.assertThat(tries.get()).isEqualTo(RETRY_TIMES);
    }

    @Test
    void shouldRetryJustOnce() {
        AtomicInteger tries = new AtomicInteger();
        RuntimeException exception = new RuntimeException("failure");
        int expectedResult = 5;

        int result = SimpleRetryWrapper.create(() -> {
            if (tries.incrementAndGet() == 1) {
                throw exception;
            }
            return expectedResult;
        }).withName(NAME).withRetryTimes(RETRY_TIMES).withRetryWaitSeconds(RETRY_WAIT_SECONDS).run();

        Assertions.assertThat(result).isEqualTo(expectedResult);
        Assertions.assertThat(tries.get()).isEqualTo(2);
    }

}
