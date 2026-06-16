package com.sequenceiq.cloudbreak.logger.concurrent;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;

class MDCCleanerScheduledExecutorTest {

    private final MDCCleanerScheduledExecutor executor =
            new MDCCleanerScheduledExecutor(1, Executors.defaultThreadFactory());

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        MDC.clear();
    }

    @Test
    void beforeExecuteGeneratesFreshRequestId() throws InterruptedException {
        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(() -> {
            capturedRequestId.set(MDC.get(LoggerContextKey.REQUEST_ID.toString()));
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);
        assertNotNull(capturedRequestId.get(), "A fresh requestId should be generated");
    }

    @Test
    void staleContextFromPreviousTaskIsCleaned() throws InterruptedException {
        CountDownLatch firstDone = new CountDownLatch(1);
        CountDownLatch secondDone = new CountDownLatch(1);
        AtomicReference<String> firstRequestId = new AtomicReference<>();
        AtomicReference<String> secondRequestId = new AtomicReference<>();
        AtomicReference<String> capturedStaleCrn = new AtomicReference<>();

        // Pool size is 1, so both tasks run on the same thread sequentially
        executor.execute(() -> {
            MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), "stale-crn-from-task1");
            firstRequestId.set(MDC.get(LoggerContextKey.REQUEST_ID.toString()));
            firstDone.countDown();
        });
        firstDone.await(5, TimeUnit.SECONDS);

        executor.execute(() -> {
            capturedStaleCrn.set(MDC.get(LoggerContextKey.RESOURCE_CRN.toString()));
            secondRequestId.set(MDC.get(LoggerContextKey.REQUEST_ID.toString()));
            secondDone.countDown();
        });
        secondDone.await(5, TimeUnit.SECONDS);

        assertNull(capturedStaleCrn.get(), "Stale resource CRN from previous task should be cleaned");
        assertNotNull(secondRequestId.get(), "Second task should have a fresh requestId");
        assertNotEquals(firstRequestId.get(), secondRequestId.get(), "Each task should get a unique requestId");
    }
}
