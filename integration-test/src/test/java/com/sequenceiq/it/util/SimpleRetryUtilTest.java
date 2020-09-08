package com.sequenceiq.it.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleRetryUtilTest {

    private static final int RETRY_WAIT_SECONDS = 0;

    private static final int RETRY_TIMES = 5;

    @Test
    void shouldSucceed() {
        int expectedResult = 5;
        AtomicInteger tries = new AtomicInteger();
        Integer result = SimpleRetryUtil.retry(RETRY_TIMES, RETRY_WAIT_SECONDS, () -> {
            tries.incrementAndGet();
            return expectedResult;
        });

        Assertions.assertThat(result).isEqualTo(expectedResult);
        Assertions.assertThat(tries.get()).isEqualTo(1);
    }

    @Test
    void shouldRetry() {
        AtomicInteger tries = new AtomicInteger();
        RuntimeException exception = new RuntimeException("failure");

        Assertions.assertThatThrownBy(() -> SimpleRetryUtil.retry(RETRY_TIMES, RETRY_WAIT_SECONDS, () -> {
            tries.incrementAndGet();
            throw exception;
        }))
                .hasMessage("Failed to run command 5 times.")
                .hasCause(exception);

        Assertions.assertThat(tries.get()).isEqualTo(RETRY_TIMES);
    }

    @Test
    void shouldRetryJustOnce() {
        AtomicInteger tries = new AtomicInteger();
        RuntimeException exception = new RuntimeException("failure");
        int expectedResult = 5;

        int result = SimpleRetryUtil.retry(RETRY_TIMES, RETRY_WAIT_SECONDS, () -> {
            if (tries.incrementAndGet() == 1) {
                throw exception;
            }
            return expectedResult;
        });

        Assertions.assertThat(result).isEqualTo(expectedResult);
        Assertions.assertThat(tries.get()).isEqualTo(2);
    }

}
