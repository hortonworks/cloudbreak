package com.sequenceiq.maintenance.dispatcher.model;

/**
 * Outcome of an HTTP dispatch to a submitter service, including optional failure detail for persistence.
 */
public record MaintenanceTaskSubmitterDispatchResult(
        MaintenanceTaskSubmitterOutcome outcome,
        String errorDetail) {

    public static MaintenanceTaskSubmitterDispatchResult success(MaintenanceTaskSubmitterOutcome outcome) {
        if (outcome == MaintenanceTaskSubmitterOutcome.FAILED) {
            throw new IllegalArgumentException("success() requires a non-FAILED outcome; use failed(errorDetail) instead");
        }
        return new MaintenanceTaskSubmitterDispatchResult(outcome, null);
    }

    public static MaintenanceTaskSubmitterDispatchResult failed(String errorDetail) {
        return new MaintenanceTaskSubmitterDispatchResult(MaintenanceTaskSubmitterOutcome.FAILED, errorDetail);
    }
}
