package com.sequenceiq.cloudbreak.service;

import java.util.function.Predicate;

import org.springframework.stereotype.Component;

/**
 * These predicates determine whether a failed operation should be retried based on the exception type and message.
 *
 * @see RetryErrorPatterns for the error message patterns used in retry decisions
 */
@Component
public class RetryPredicates {

    private final RetryErrorPatterns retryErrorPatterns;

    public RetryPredicates(RetryErrorPatterns retryErrorPatterns) {
        this.retryErrorPatterns = retryErrorPatterns;
    }

    /**
     * Retry all exceptions (default behavior).
     */
    public Predicate<Exception> retryAll() {
        return ex -> true;
    }

    /**
     * Never retry any exception.
     */
    public Predicate<Exception> retryNone() {
        return ex -> false;
    }

    /**
     * Retry all errors except specific Salt execution failures that indicate permanent failures.
     */
    public Predicate<Exception> retryTransientErrors() {
        return ex -> !retryErrorPatterns.containsNonRetryableError(ex.getMessage());
    }
}

