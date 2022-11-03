package com.sequenceiq.cloudbreak.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Streams;
import com.google.common.util.concurrent.MoreExecutors;

public class PromiseTest {

    private ExecutorService executorService;

    @BeforeEach
    public void setUp() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    public void tearDown() {
        MoreExecutors.shutdownAndAwaitTermination(executorService, Duration.ofSeconds(100));
    }

    @Test
    public void testThrowsExceptionIfNotAccepted() {
        Promise<String> underTest = Promise.prepare();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.await(2, TimeUnit.SECONDS));

        assertEquals("java.util.concurrent.TimeoutException", exception.getMessage());
        assertFalse(underTest.isComplete());
    }

    @Test
    public void testAccepted() throws InterruptedException {
        Promise<String> underTest = Promise.prepare();

        executorService.submit(() -> {
            sleepMs(100);
            underTest.accept("OK");
        });

        assertEquals("OK", underTest.await());
        assertTrue(underTest.isComplete());
    }

    @Test
    public void testMultipleAccepted() {
        List<Promise<String>> underTests = createN(100, Promise::prepare);

        underTests.forEach(promise -> executorService.submit(() -> {
            sleepMs(100);
            promise.accept("OK");
        }));

        underTests.forEach(promise -> {
            try {
                assertEquals("OK", promise.await());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testMultipleAcceptAndOnErrorCall() {
        int n = 1000;
        List<Promise<String>> underTests = createN(n, Promise::prepare);
        List<Boolean> shouldThrowStates = createN(n, () -> ThreadLocalRandom.current().nextBoolean());
        long expectedExceptionCount = shouldThrowStates.stream().filter(Boolean.TRUE::equals).count();
        long expectedOkCount = shouldThrowStates.stream().filter(Boolean.FALSE::equals).count();

        Streams.zip(underTests.stream(), shouldThrowStates.stream(), Pair::of)
                .forEach(v -> {
                    executorService.submit(() -> {
                        if (v.getValue()) {
                            v.getKey().onError(new RuntimeException("Not ok."));
                        } else {
                            v.getKey().accept("OK");
                        }
                    });
                });

        AtomicLong exceptionCount = new AtomicLong();
        AtomicLong okCount = new AtomicLong();
        underTests.forEach(promise -> {
            try {
                promise.await();
                okCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            }
        });

        assertEquals(expectedExceptionCount, exceptionCount.get());
        assertEquals(expectedOkCount, okCount.get());
        assertTrue(underTests.stream().allMatch(Promise::isComplete));
    }

    @Test
    public void testMultipleOnNextAndOnErrorCall() {
        int n = 1000;
        List<Promise<String>> underTests = createN(n, Promise::prepare);
        List<Boolean> shouldThrowStates = createN(n, () -> ThreadLocalRandom.current().nextBoolean());
        long expectedExceptionCount = shouldThrowStates.stream().filter(Boolean.TRUE::equals).count();
        long expectedOkCount = shouldThrowStates.stream().filter(Boolean.FALSE::equals).count();

        Streams.zip(underTests.stream(), shouldThrowStates.stream(), Pair::of)
                .forEach(v -> {
                    executorService.submit(() -> {
                        if (v.getValue()) {
                            v.getKey().onError(new RuntimeException("Not ok."));
                        } else {
                            v.getKey().onNext("OK");
                        }
                    });
                });

        AtomicLong exceptionCount = new AtomicLong();
        AtomicLong okCount = new AtomicLong();
        underTests.forEach(promise -> {
            try {
                promise.await();
                okCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            }
        });

        assertEquals(expectedExceptionCount, exceptionCount.get());
        assertEquals(expectedOkCount, okCount.get());
        assertTrue(underTests.stream().allMatch(Promise::isComplete));
    }

    private static void sleepMs(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> List<T> createN(int n, Supplier<T> supplier) {
        return IntStream.range(0, n)
                .boxed()
                .map(v -> supplier.get())
                .collect(Collectors.toList());
    }
}