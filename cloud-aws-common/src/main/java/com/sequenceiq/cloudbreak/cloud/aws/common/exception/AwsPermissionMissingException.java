package com.sequenceiq.cloudbreak.cloud.aws.common.exception;

import java.util.List;

public class AwsPermissionMissingException extends Exception {

    private final List<String> failedActions;

    public AwsPermissionMissingException() {
        this.failedActions = List.of();
    }

    public AwsPermissionMissingException(String message) {
        super(message);
        this.failedActions = List.of();
    }

    public AwsPermissionMissingException(String message, List<String> failedActions) {
        super(message);
        this.failedActions = failedActions == null ? List.of() : List.copyOf(failedActions);
    }

    public List<String> getFailedActions() {
        return failedActions;
    }
}
