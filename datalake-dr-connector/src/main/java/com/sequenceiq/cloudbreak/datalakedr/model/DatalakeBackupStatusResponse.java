package com.sequenceiq.cloudbreak.datalakedr.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class DatalakeBackupStatusResponse implements DatalakeOperationStatus {

    private final State state;

    private final String backupId;

    private final List<String> includedData;

    private final String timestamp;

    private final Optional<String> failureReason;

    public DatalakeBackupStatusResponse(String backupId, State state, List<String> includedData, String timestamp, String failureReason) {
        this.backupId = backupId;
        this.state = state;
        this.includedData = includedData;
        this.timestamp = timestamp;
        this.failureReason = Optional.ofNullable(failureReason).filter(Predicate.not("null"::equals));
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public String getFailureReason() {
        return failureReason.orElse(NO_FAILURES);
    }

    public String getBackupId() {
        return backupId;
    }

    @Override
    public List<String> getIncludedData() {
        return includedData;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
