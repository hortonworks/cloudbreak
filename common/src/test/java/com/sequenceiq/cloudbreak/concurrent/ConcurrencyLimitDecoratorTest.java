package com.sequenceiq.cloudbreak.concurrent;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class ConcurrencyLimitDecoratorTest {

    private static final int CONCURRENCY_LIMIT = 100;

    private static final int NUMBER_OF_TASKS = 500;

    private static final long TASK_SLEEP_MS = 200;

    // NUMBER_OF_TASKS / CONCURRENCY_LIMIT = 5 waves, each gated by TASK_SLEEP_MS, so the decorator
    // forces a hard floor of ~5 * 200ms = 1000ms (vs ~200ms if it failed to throttle).
    private static final long MIN_EXPECTED_DURATION_MS = 900;

    @Test
    public void testConcurrencyLimitDecoratorWithVirtualThreads() {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            ConcurrencyLimitDecorator concurrencyLimitDecorator = new ConcurrencyLimitDecorator(CONCURRENCY_LIMIT);
            long startTime = System.currentTimeMillis();
            List<Future> list = IntStream.range(0, NUMBER_OF_TASKS)
                    .boxed()
                    .map(i -> executorService.submit(concurrencyLimitDecorator.apply(() -> {
                        Thread.sleep(TASK_SLEEP_MS);
                        return i;
                    })))
                    .toList();
            list.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
            long duration = System.currentTimeMillis() - startTime;
            assertThat(duration, Matchers.greaterThan(MIN_EXPECTED_DURATION_MS));
        }
    }
}