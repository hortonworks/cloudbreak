package com.sequenceiq.cloudbreak.service;

import java.util.function.Supplier;

public interface Retry {

    void testWith2SecDelayMax5Times(Runnable action) throws ActionFailedException;

    Boolean testWith2SecDelayMax5Times(Supplier<Boolean> action) throws ActionFailedException;

    <T> T testWith2SecDelayMax15Times(Supplier<T> action) throws ActionFailedException;

    <T> T testWith1SecDelayMax5Times(Supplier<T> action) throws ActionFailedException;

    <T> T testWith1SecDelayMax5TimesMaxDelay5MinutesMultiplier5(Supplier<T> action) throws ActionFailedException;

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

        public static Runnable wrapRte(Runnable action) {
            return () -> {
                try {
                    action.run();
                } catch (RuntimeException e) {
                    throw new ActionFailedException(e);
                }
            };
        }

        public static <T> Supplier<T> wrapRte(Supplier<T> action) {
            return () -> {
                try {
                    return action.get();
                } catch (RuntimeException e) {
                    throw new ActionFailedException(e);
                }
            };
        }

    }
}
