package com.sequenceiq.cloudbreak.job.stackpatcher;

import com.sequenceiq.cloudbreak.service.CloudbreakException;

public class UnknownStackPatchTypeException extends CloudbreakException {
    public UnknownStackPatchTypeException(String message) {
        super(message);
    }

    public UnknownStackPatchTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownStackPatchTypeException(Throwable cause) {
        super(cause);
    }
}
