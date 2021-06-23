package com.sequenceiq.environment.api.v1.environment.model.base;

public enum LoadBalancerUpdateStatus {
    NOT_STARTED,
    IN_PROGRESS,
    FAILED,
    FINISHED,
    COULD_NOT_START,
    AMBIGUOUS;

    public static boolean isErrorCase(LoadBalancerUpdateStatus status) {
        return status == FAILED ||
            status == COULD_NOT_START;
    }
}
