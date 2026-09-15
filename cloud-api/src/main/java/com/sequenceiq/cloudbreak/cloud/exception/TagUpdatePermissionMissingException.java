package com.sequenceiq.cloudbreak.cloud.exception;

import java.util.List;

/**
 * Thrown when a pre-flight IAM/permission check for updating user-defined tags on
 * existing cloud resources determines that the cloud credential is missing one or
 * more required actions. The message is expected to be human-readable and safe to
 * surface to the API caller; {@link #getFailedActions()} carries the individual
 * "action : resource" pairs for structured presentation.
 */
public class TagUpdatePermissionMissingException extends Exception {

    private final List<String> failedActions;

    public TagUpdatePermissionMissingException(String message, Throwable cause) {
        super(message, cause);
        failedActions = List.of();
    }

    public TagUpdatePermissionMissingException(String message, List<String> failedActions, Throwable cause) {
        super(message, cause);
        this.failedActions = failedActions == null ? List.of() : List.copyOf(failedActions);
    }

    public List<String> getFailedActions() {
        return failedActions;
    }
}
