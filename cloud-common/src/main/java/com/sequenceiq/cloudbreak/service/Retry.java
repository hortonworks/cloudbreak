package com.sequenceiq.cloudbreak.service;

import java.util.function.Supplier;

public interface Retry {

    Boolean testWith2SecDelayMax5Times(Supplier<Boolean> action) throws ActionWentFail;

    class ActionWentFail extends RuntimeException {
    }
}
