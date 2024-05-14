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

    private static final int CONCURRENCY_LIMIT = 1000;

    private static final int NUMBER_OF_TASKS = 5000;

    @Test
    public void testConcurrencyLimitDecoratorWithVirtualThreads() {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            ConcurrencyLimitDecorator concurrencyLimitDecorator = new ConcurrencyLimitDecorator(CONCURRENCY_LIMIT);
            long startTime = System.currentTimeMillis();
            List<Future> list = IntStream.range(0, NUMBER_OF_TASKS)
                    .boxed()
                    .map(i -> executorService.submit(concurrencyLimitDecorator.apply(() -> {
                        Thread.sleep(1000);
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
            assertThat(duration, Matchers.greaterThan(4500L));
        }
    }
}