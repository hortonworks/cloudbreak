package com.sequenceiq.cloudbreak.service;

import java.util.Set;

public class ScalingException extends CloudbreakException {

    private final Set<String> failedInstanceIds;

    public ScalingException(String message, Set<String> failedInstanceIds) {
        super(message);
        this.failedInstanceIds = failedInstanceIds;
    }

    public ScalingException(String message, Throwable cause, Set<String> failedInstanceIds) {
        super(message, cause);
        this.failedInstanceIds = failedInstanceIds;
    }

    public ScalingException(Throwable cause, Set<String> failedInstanceIds) {
        super(cause);
        this.failedInstanceIds = failedInstanceIds;
    }

    public Set<String> getFailedInstanceIds() {
        return failedInstanceIds;
    }
}
