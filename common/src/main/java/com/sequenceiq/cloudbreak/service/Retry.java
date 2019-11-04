package com.sequenceiq.cloudbreak.service;

import java.util.function.Supplier;

public interface Retry {

    Boolean testWith2SecDelayMax5Times(Supplier<Boolean> action) throws ActionFailedException;

    <T> T testWith2SecDelayMax15Times(Supplier<T> action) throws ActionFailedException;

    class ActionFailedException extends RuntimeException {
        public ActionFailedException() {
        }

        public ActionFailedException(String message) {
            super(message);
        }
    }
}
