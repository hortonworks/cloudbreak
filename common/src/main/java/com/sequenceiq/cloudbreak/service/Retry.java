package com.sequenceiq.cloudbreak.service;

import java.util.function.Supplier;

public interface Retry {

    void testWith2SecDelayMax5Times(Runnable action) throws ActionFailedException;

    Boolean testWith2SecDelayMax5Times(Supplier<Boolean> action) throws ActionFailedException;

    <T> T testWith2SecDelayMax15Times(Supplier<T> action) throws ActionFailedException;

    <T> T testWith1SecDelayMax5Times(Supplier<T> action) throws ActionFailedException;

    class ActionFailedException extends RuntimeException {
        public ActionFailedException() {
        }

        public ActionFailedException(String message) {
            super(message);
        }
    }
}
