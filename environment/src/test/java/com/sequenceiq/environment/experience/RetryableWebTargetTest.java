package com.sequenceiq.environment.experience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

class RetryableWebTargetTest {

    private static final long RETRYABLE_BACKOFF_DELAY = 500L;

    private static final int RETRYABLE_MAX_ATTEMPT = 5;

    private static final String GET_METHOD_NAME = "get";

    private static final String DELETE_METHOD_NAME = "delete";

    private RetryableWebTarget underTest;

    @BeforeEach
    void setUp() {
        underTest = new RetryableWebTarget();
    }

    @Test
    void testGetHasRetryableAnnotationWithMaxAttemptWithExpectedAmount() throws NoSuchMethodException {
        Retryable retryableAnnotation = getRetryableAnnotationForMethod(GET_METHOD_NAME);

        assertEquals(RETRYABLE_MAX_ATTEMPT, retryableAnnotation.maxAttempts());
    }

    @Test
    void testDeleteHasRetryableAnnotationWithMaxAttemptWithExpectedAmount() throws NoSuchMethodException {
        Retryable retryableAnnotation = getRetryableAnnotationForMethod(DELETE_METHOD_NAME);

        assertEquals(RETRYABLE_MAX_ATTEMPT, retryableAnnotation.maxAttempts());
    }

    @Test
    void testGetHasRetryableAnnotationWithBackoffWithExpectedAmount() throws NoSuchMethodException {
        Backoff backoff = getRetryableAnnotationForMethod(GET_METHOD_NAME).backoff();

        assertEquals(RETRYABLE_BACKOFF_DELAY, backoff.delay());
    }

    @Test
    void testDeleteHasRetryableAnnotationWithBackoffWithExpectedAmount() throws NoSuchMethodException {
        Backoff backoff = getRetryableAnnotationForMethod(DELETE_METHOD_NAME).backoff();

        assertEquals(RETRYABLE_BACKOFF_DELAY, backoff.delay());
    }

    @Test
    void testWhenPassingAnInvocationBuilderToGetThenGetCallShouldHappenOnIt() {
        Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
        Response expectedResult = mock(Response.class);
        when(mockBuilder.get()).thenReturn(expectedResult);

        Response result = underTest.get(mockBuilder);

        assertEquals(expectedResult, result);
        verify(mockBuilder, times(1)).get();
    }

    @Test
    void testWhenPassingAnInvocationBuilderToDeleteThenDeleteCallShouldHappenOnIt() {
        Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
        Response expectedResult = mock(Response.class);
        when(mockBuilder.delete()).thenReturn(expectedResult);

        Response result = underTest.delete(mockBuilder);

        assertEquals(expectedResult, result);
        verify(mockBuilder, times(1)).delete();
    }

    private Retryable getRetryableAnnotationForMethod(String methodName) throws NoSuchMethodException {
        Method getMethod = underTest.getClass().getMethod(methodName, Invocation.Builder.class);
        return getMethod.getAnnotation(Retryable.class);
    }

}
