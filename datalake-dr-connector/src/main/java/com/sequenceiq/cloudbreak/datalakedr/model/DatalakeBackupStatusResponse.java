package com.sequenceiq.cloudbreak.datalakedr.model;

import java.util.Optional;

public class DatalakeBackupStatusResponse {

    public static final String NO_FAILURES = "No failure messages found";

    public enum State {
        STARTED,
        IN_PROGRESS,
        SUCCESSFUL,
        VALIDATION_SUCCESSFUL,
        VALIDATION_FAILED,
        FAILED,
        CANCELLED
    }

    private final State state;

    private final String backupId;

    private final Optional<String> failureReason;

    public DatalakeBackupStatusResponse(String backupId, State state, Optional<String> failureReason) {
        this.backupId = backupId;
        this.state = state;
        this.failureReason = failureReason.isPresent() && "null".equals(failureReason.get())
            ? Optional.empty() : failureReason;
    }

    public boolean isComplete() {
        return state != State.IN_PROGRESS && state != State.STARTED;
    }

    public boolean failed() {
        return state.equals(State.FAILED) || state.equals(State.VALIDATION_FAILED);
    }

    public State getState() {
        return state;
    }

    public String getFailureReason() {
        return failureReason.orElse(NO_FAILURES);
    }

    public String getBackupId() {
        return backupId;
    }
}
