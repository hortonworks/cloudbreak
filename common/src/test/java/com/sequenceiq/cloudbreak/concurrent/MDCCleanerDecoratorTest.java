package com.sequenceiq.cloudbreak.concurrent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;

class MDCCleanerDecoratorTest {

    private final MDCCleanerDecorator decorator = new MDCCleanerDecorator();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void decorateCleansStaleMdcAndGeneratesFreshRequestId() {
        MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), "stale-crn");
        MDC.put(LoggerContextKey.TENANT.toString(), "stale-tenant");

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        AtomicReference<String> capturedResourceCrn = new AtomicReference<>();

        Runnable decorated = decorator.decorate(() -> {
            capturedRequestId.set(MDC.get(LoggerContextKey.REQUEST_ID.toString()));
            capturedResourceCrn.set(MDC.get(LoggerContextKey.RESOURCE_CRN.toString()));
        });
        decorated.run();

        assertNotNull(capturedRequestId.get(), "A fresh requestId should be generated");
        assertNull(capturedResourceCrn.get(), "Stale resource CRN should be cleaned");
    }

    @Test
    void decorateCleansMdcAfterExecution() {
        Runnable decorated = decorator.decorate(() -> {
            MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), "task-crn");
        });
        decorated.run();

        assertNull(MDC.get(LoggerContextKey.RESOURCE_CRN.toString()), "MDC should be cleaned after execution");
        assertNull(MDC.get(LoggerContextKey.REQUEST_ID.toString()), "RequestId should be cleaned after execution");
    }

    @Test
    void applyCleansStaleMdcAndGeneratesFreshRequestId() throws Exception {
        MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), "stale-crn");

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        AtomicReference<String> capturedResourceCrn = new AtomicReference<>();

        @SuppressWarnings("unchecked")
        java.util.concurrent.Callable<Void> decorated = decorator.apply(() -> {
            capturedRequestId.set(MDC.get(LoggerContextKey.REQUEST_ID.toString()));
            capturedResourceCrn.set(MDC.get(LoggerContextKey.RESOURCE_CRN.toString()));
            return null;
        });
        decorated.call();

        assertNotNull(capturedRequestId.get(), "A fresh requestId should be generated");
        assertNull(capturedResourceCrn.get(), "Stale resource CRN should be cleaned");
    }

    @Test
    void applyCleansMdcAfterExecution() throws Exception {
        @SuppressWarnings("unchecked")
        java.util.concurrent.Callable<Void> decorated = decorator.apply(() -> {
            MDC.put(LoggerContextKey.RESOURCE_CRN.toString(), "task-crn");
            return null;
        });
        decorated.call();

        assertNull(MDC.get(LoggerContextKey.RESOURCE_CRN.toString()), "MDC should be cleaned after execution");
        assertNull(MDC.get(LoggerContextKey.REQUEST_ID.toString()), "RequestId should be cleaned after execution");
    }
}
