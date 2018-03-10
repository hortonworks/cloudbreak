package com.sequenceiq.cloudbreak.service;

import java.util.function.Supplier;

public interface Retry {

    Boolean testWith2SecDelayMax5Times(Supplier<Boolean> action) throws ActionWentFailException;

    class ActionWentFailException extends RuntimeException {
        public ActionWentFailException() {
        }

        public ActionWentFailException(String message) {
            super(message);
        }
    }
}
