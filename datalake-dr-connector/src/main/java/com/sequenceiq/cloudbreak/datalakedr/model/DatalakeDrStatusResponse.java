package com.sequenceiq.cloudbreak.datalakedr.model;

import java.util.Optional;

public class DatalakeDrStatusResponse {

    public enum State {
        STARTED,
        IN_PROGRESS,
        SUCCESSFUL,
        FAILED
    }

    private final State state;

    private final Optional<String> failureReason;

    public DatalakeDrStatusResponse(State state, Optional<String> failureReason) {
        this.state = state;
        this.failureReason = failureReason.isPresent() && "null".equals(failureReason.get())
            ? Optional.empty() : failureReason;
    }

    public boolean isComplete() {
        return state == State.SUCCESSFUL || state == State.FAILED;
    }

    public State getState() {
        return state;
    }

    public Optional<String> getFailureReason() {
        return failureReason;
    }
}
