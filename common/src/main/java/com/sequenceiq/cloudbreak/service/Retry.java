package com.sequenceiq.cloudbreak.service;

import java.util.function.Supplier;

public interface Retry {

    void testWith2SecDelayMax5Times(Runnable action) throws ActionFailedException;

    <T> T testWith2SecDelayMax5Times(Supplier<T> action) throws ActionFailedException;

    <T> T testWith2SecDelayMax15Times(Supplier<T> action) throws ActionFailedException;

    <T> T testWith1SecDelayMax5Times(Supplier<T> action) throws ActionFailedException;

    <T> T testWith1SecDelayMax3Times(Supplier<T> action) throws ActionFailedException;

    <T> T testWith1SecDelayMax5TimesMaxDelay5MinutesMultiplier5(Supplier<T> action) throws ActionFailedException;

    <T> T testWith1SecDelayMax5TimesWithCheckRetriable(Supplier<T> action) throws ActionFailedException;

    <T> T testWithoutRetry(Supplier<T> action) throws ActionFailedException;

    class ActionFailedException extends RuntimeException {
        public ActionFailedException() {
        }

        public ActionFailedException(String message) {
            super(message);
        }

        public ActionFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        public ActionFailedException(Throwable cause) {
            super(cause);
        }

        public ActionFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

        public static ActionFailedException ofCause(Throwable cause) {
            return new ActionFailedException(cause != null ? cause.getMessage() : null, cause);
        }
    }

    class ActionFailedNonRetryableException extends RuntimeException {
        public ActionFailedNonRetryableException() {
        }

        public ActionFailedNonRetryableException(String message) {
            super(message);
        }

        public ActionFailedNonRetryableException(String message, Throwable cause) {
            super(message, cause);
        }

        public ActionFailedNonRetryableException(Throwable cause) {
            super(cause);
        }

        public ActionFailedNonRetryableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

        public static ActionFailedNonRetryableException ofCause(Throwable cause) {
            return new ActionFailedNonRetryableException(cause != null ? cause.getMessage() : null, cause);
        }
    }
}
