package com.sequenceiq.environment.exception;

import java.util.List;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

/**
 * Raised when the pre-flight tag-update permission check rejects an environment tag PUT.
 * Extends {@link BadRequestException} so the response is HTTP 400; the accompanying
 * {@link #getFailedActions()} list is exposed to the UI via a dedicated exception mapper
 * that packs it into {@code ExceptionResponse.payload}.
 */
public class EnvironmentTagUpdatePermissionMissingException extends BadRequestException {

    private final List<String> failedActions;

    public EnvironmentTagUpdatePermissionMissingException(String message, List<String> failedActions, Throwable cause) {
        super(message, cause);
        this.failedActions = failedActions == null ? List.of() : List.copyOf(failedActions);
    }

    public List<String> getFailedActions() {
        return failedActions;
    }
}
