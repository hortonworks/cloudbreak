package com.sequenceiq.cloudbreak.datalakedr.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class DatalakeRestoreStatusResponse implements DatalakeOperationStatus {

    private final String restoreId;

    private final String backupId;

    private final State state;

    private final Optional<String> failureReason;

    public DatalakeRestoreStatusResponse(String backupId, String restoreId, State state, String failureReason) {
        this.backupId = backupId;
        this.restoreId = restoreId;
        this.state = state;
        this.failureReason = Optional.ofNullable(failureReason).filter(Predicate.not("null"::equals));
    }

    public String getRestoreId() {
        return restoreId;
    }

    @Override
    public State getState() {
        return state;
    }

    public String getFailureReason() {
        return failureReason.orElse(NO_FAILURES);
    }

    public String getBackupId() {
        return backupId;
    }

    public DatalakeBackupStatusResponse toBackupStatusResponse() {
        return new DatalakeBackupStatusResponse(backupId, state, List.of(), "", failureReason.orElse(null));
    }
}
