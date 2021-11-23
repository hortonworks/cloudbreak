package com.sequenceiq.cloudbreak.service.stackpatch;

public class ExistingStackPatchApplyException extends Exception {

    public ExistingStackPatchApplyException(String message) {
        super(message);
    }

    public ExistingStackPatchApplyException(String message, Throwable cause) {
        super(message, cause);
    }
}
