package com.sequenceiq.cloudbreak.domain;

import java.util.Arrays;
import java.util.List;

public enum Status {
    REQUESTED,
    CREATE_IN_PROGRESS,
    AVAILABLE,
    UPDATE_IN_PROGRESS,
    UPDATE_REQUESTED,
    UPDATE_FAILED,
    CREATE_FAILED,
    ENABLE_SECURITY_FAILED,
    DELETE_IN_PROGRESS,
    DELETE_FAILED,
    DELETE_COMPLETED,
    STOPPED,
    STOP_REQUESTED,
    START_REQUESTED,
    STOP_IN_PROGRESS,
    START_IN_PROGRESS,
    START_FAILED,
    STOP_FAILED;

    public static List<Status> stopStatusesForUpdate() {
        return Arrays.asList(START_FAILED, START_IN_PROGRESS, START_REQUESTED);
    }

    public static List<Status> availableStatusesForUpdate() {
        return Arrays.asList(REQUESTED, CREATE_IN_PROGRESS, UPDATE_IN_PROGRESS, UPDATE_REQUESTED,
                UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, STOP_REQUESTED, STOP_IN_PROGRESS, STOP_FAILED);
    }

    public String normalizedStatusName() {
        return name().replaceAll("_", " ").toLowerCase();
    }
}
