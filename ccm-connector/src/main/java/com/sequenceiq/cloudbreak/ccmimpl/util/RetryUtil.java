package com.sequenceiq.cloudbreak.ccmimpl.util;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.StopStrategy;
import com.github.rholder.retry.WaitStrategies;
import com.github.rholder.retry.WaitStrategy;
import com.google.common.base.Throwables;
import com.sequenceiq.cloudbreak.ccm.exception.CcmException;

/**
 * Provides utilities for retrying.
 */
public class RetryUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private RetryUtil() {
    }

    /**
     * Performs the specified action with retries.
     *
     * @param action                   the action to perform
     * @param actionDescription        a description of the action being performed
     * @param waitUntilTime            the latest time to wait to perform the action
     * @param sleepTimeMs              the sleep time in millisecond for retries
     * @param exceptionClass           the class of exception to throw
     * @param timeoutExceptionSupplier a supplier of exceptions for when all attempts fail
     * @param logger                   the logger
     * @param <T>                      the type of object returned
     * @param <E>                      the type of exception thrown
     * @return the result object
     * @throws InterruptedException if the thread is interrupted
     * @throws E                    if the operation fails
     */
    public static <T, E extends CcmException> T performWithRetries(
            Callable<T> action, String actionDescription, ZonedDateTime waitUntilTime, long sleepTimeMs,
            Class<E> exceptionClass, Supplier<E> timeoutExceptionSupplier,
            Logger logger)
            throws InterruptedException, E {
        Retryer<T> loop = buildRetryer(
                actionDescription,
                waitUntilTime,
                sleepTimeMs,
                exceptionClass,
                logger);

        T result;
        try {
            result = loop.call(action);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            Throwables.throwIfInstanceOf(cause, InterruptedException.class);
            Throwables.throwIfUnchecked(cause);
            throw new IllegalStateException(cause);
        } catch (RetryException e) {
            if (Thread.interrupted()) {
                throw new InterruptedException(String.format(
                        "Interrupted while trying to %s", actionDescription));
            }

            handleAllAttemptsFailed(e, actionDescription, exceptionClass, timeoutExceptionSupplier, logger);
            // unreachable
            throw new IllegalStateException(e);
        }
        return result;
    }

    /**
     * Throws an appropriate exception when all attempts failed.
     *
     * @param e                 the retry exception
     * @param actionDescription a description of the action being performed
     * @throws InterruptedException if the action was interrupted
     * @throws E                    if the action timed out
     */
    private static <E extends CcmException> void handleAllAttemptsFailed(
            RetryException e, String actionDescription, Class<E> exceptionClass, Supplier<E> exceptionSupplier, Logger logger) throws InterruptedException, E {
        logger.info("Failed to {}", actionDescription);
        Attempt<?> lastFailedAttempt = e.getLastFailedAttempt();
        if (lastFailedAttempt.hasException()) {
            Throwable cause = lastFailedAttempt.getExceptionCause();
            Throwables.throwIfInstanceOf(cause, exceptionClass);
            Throwables.throwIfInstanceOf(cause, InterruptedException.class);
            Throwables.throwIfUnchecked(cause);
            throw new IllegalStateException(cause);
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Builds a retryer.
     *
     * @param actionDescription a description of the action being performed
     * @param waitUntilTime     the latest time to wait to perform the action
     * @param sleepTimeMs       the sleep time in millisecond for retries
     * @param exceptionClass    the class of exception to throw
     * @param logger            the logger
     * @param <T>               the type of object returned
     * @return the retryer
     */
    private static <T> Retryer<T> buildRetryer(String actionDescription, ZonedDateTime waitUntilTime, long sleepTimeMs,
            Class<? extends CcmException> exceptionClass, Logger logger) {
        StopStrategy stop;
        if (waitUntilTime != null) {
            // Given the time at which waiting should stop,
            // get the available number of millis from this instant
            stop = StopStrategyFactory.waitUntilDateTime(waitUntilTime);
            logger.info("Trying until {} to {}", waitUntilTime, actionDescription);
        } else {
            stop = StopStrategies.neverStop();
            logger.warn("Unbounded wait to {}", actionDescription);
        }

        WaitStrategy wait = WaitStrategies.fixedWait(sleepTimeMs, TimeUnit.MILLISECONDS);
        logger.info("Checking every {} milliseconds", sleepTimeMs);

        return RetryerBuilder.<T>newBuilder()
                .retryIfException(t -> exceptionClass.isInstance(t) && ((CcmException) t).isRetryable())
                .retryIfResult(Objects::isNull)
                .withStopStrategy(stop)
                .withWaitStrategy(wait)
                .build();
    }
}
