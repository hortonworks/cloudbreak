package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;

public class SyncStatusDetail {

    private String environmentCrn;

    private SynchronizationStatus status;

    private String details;

    private ImmutableMultimap<String, String> warnings;

    public SyncStatusDetail(String environmentCrn, SynchronizationStatus status, String details, Multimap<String, String> warnings) {
        this.environmentCrn = requireNonNull(environmentCrn);
        this.status = requireNonNull(status);
        this.details = requireNonNull(details);
        this.warnings = ImmutableMultimap.copyOf(requireNonNull(warnings));
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

    public ImmutableMultimap<String, String> getWarnings() {
        return warnings;
    }

    public static SyncStatusDetail fail(String environmentCrn, String failureMessage, Multimap<String, String> warnings) {
        return new SyncStatusDetail(environmentCrn, SynchronizationStatus.FAILED, failureMessage, warnings);
    }

    public static SyncStatusDetail succeed(String environmentCrn) {
        return new SyncStatusDetail(environmentCrn, SynchronizationStatus.COMPLETED, "sync completed successfully", ImmutableMultimap.of());
    }
}
