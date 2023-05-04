package com.sequenceiq.cloudbreak.datalakedr.model;

import java.util.List;

public interface DatalakeOperationStatus {
    String NO_FAILURES = "No failure messages found";

    enum State {
        STARTED,
        IN_PROGRESS,
        SUCCESSFUL,
        VALIDATION_SUCCESSFUL,
        VALIDATION_FAILED,
        FAILED,
        CANCELLED;

        public boolean isComplete() {
            switch (this) {
                case IN_PROGRESS:
                case STARTED:
                    return false;
                default:
                    return true;
            }
        }

        public boolean isNotComplete() {
            return !isComplete();
        }

        public boolean isFailed() {
            switch (this) {
                case FAILED:
                case VALIDATION_FAILED:
                    return true;
                default:
                    return false;
            }
        }

    }

    State getState();

    List<String> getIncludedData();

    String getFailureReason();

    default boolean isComplete() {
        return getState().isComplete();
    }

    default boolean isFailed() {
        return getState().isFailed();
    }

}
