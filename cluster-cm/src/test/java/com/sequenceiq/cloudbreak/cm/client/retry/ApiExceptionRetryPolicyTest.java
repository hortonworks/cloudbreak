package com.sequenceiq.cloudbreak.cm.client.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.retry.RetryContext;

import com.cloudera.api.swagger.client.ApiException;

public class ApiExceptionRetryPolicyTest {

    private static final int RETRY_LIMIT = 5;

    @Test
    public void testCanRetryIfNoException() {
        ApiExceptionRetryPolicy policy = new ApiExceptionRetryPolicy();
        RetryContext context = policy.open(null);
        assertTrue(policy.canRetry(context));
    }

    @Test
    public void testEmptyExceptionsNeverRetry() {

        ApiExceptionRetryPolicy policy = new ApiExceptionRetryPolicy();
        RetryContext context = policy.open(null);

        policy.registerThrowable(context, new IllegalStateException());
        assertFalse(policy.canRetry(context));
    }

    @Test
    public void testWithExceptionDefaultAlwaysRetry() {

        ApiExceptionRetryPolicy policy = new ApiExceptionRetryPolicy();
        RetryContext context = policy.open(null);

        ApiException apiException = mock(ApiException.class);
        when(apiException.getCode()).thenReturn(402);

        // ...so we can't retry this one...
        policy.registerThrowable(context, apiException);
        assertFalse(policy.canRetry(context));

        // ...and we can't retry this one...
        when(apiException.getCode()).thenReturn(455);
        policy.registerThrowable(context, apiException);
        assertFalse(policy.canRetry(context));

        // ...and we can retry this one...
        when(apiException.getCode()).thenReturn(502);
        policy.registerThrowable(context, apiException);
        assertTrue(policy.canRetry(context));

        // ...and we can retry this one...
        when(apiException.getCode()).thenReturn(401);
        policy.registerThrowable(context, apiException);
        assertTrue(policy.canRetry(context));

        // ...and we can retry this one...
        when(apiException.getCode()).thenReturn(0);
        policy.registerThrowable(context, apiException);
        assertTrue(policy.canRetry(context));
    }

    @Test
    public void testRetryLimitSubsequentState() {

        ApiException apiException = mock(ApiException.class);
        when(apiException.getCode()).thenReturn(500);
        ApiExceptionRetryPolicy policy = new ApiExceptionRetryPolicy(2);
        RetryContext context = policy.open(null);
        assertTrue(policy.canRetry(context));
        policy.registerThrowable(context, apiException);
        assertTrue(policy.canRetry(context));
        policy.registerThrowable(context, apiException);
        policy.registerThrowable(context, apiException);
        assertFalse(policy.canRetry(context));
    }

    @Test
    public void testRetryCount() {
        ApiException apiException = mock(ApiException.class);
        when(apiException.getCode()).thenReturn(401);
        when(apiException.getMessage()).thenReturn("foo");
        ApiExceptionRetryPolicy policy = new ApiExceptionRetryPolicy();
        RetryContext context = policy.open(null);
        assertNotNull(context);
        policy.registerThrowable(context, null);
        assertEquals(0, context.getRetryCount());
        policy.registerThrowable(context, apiException);
        assertEquals(1, context.getRetryCount());
        assertEquals("foo", context.getLastThrowable().getMessage());
    }

    @Test
    public void testDefaultRetryLimit() {
        ApiException apiException = mock(ApiException.class);
        when(apiException.getCode()).thenReturn(500);
        ApiExceptionRetryPolicy policy = new ApiExceptionRetryPolicy();
        RetryContext context = policy.open(null);
        assertNotNull(context);
        for (int i = 0; i < RETRY_LIMIT; i++) {
            policy.registerThrowable(context, apiException);
            assertTrue(policy.canRetry(context));
        }
        policy.registerThrowable(context, apiException);
        assertFalse(policy.canRetry(context));
    }

    @Test
    public void testParent() {
        ApiExceptionRetryPolicy policy = new ApiExceptionRetryPolicy();
        RetryContext context = policy.open(null);
        RetryContext child = policy.open(context);
        assertNotSame(child, context);
        assertSame(context, child.getParent());
    }
}