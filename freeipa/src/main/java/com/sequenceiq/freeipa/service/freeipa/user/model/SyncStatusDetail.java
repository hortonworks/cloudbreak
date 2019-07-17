package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;

public class SyncStatusDetail {

    private String environmentCrn;

    private SynchronizationStatus status;

    private String details;

    public SyncStatusDetail(String environmentCrn, SynchronizationStatus status, String details) {
        this.environmentCrn = requireNonNull(environmentCrn);
        this.status = requireNonNull(status);
        this.details = requireNonNull(details);
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public SynchronizationStatus getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }

    public static SyncStatusDetail fail(String environmentCrn, String failureMessage) {
        return new SyncStatusDetail(environmentCrn, SynchronizationStatus.FAILED, failureMessage);
    }

    public static SyncStatusDetail succeed(String environmentCrn, String details) {
        return new SyncStatusDetail(environmentCrn, SynchronizationStatus.COMPLETED, details);
    }
}
