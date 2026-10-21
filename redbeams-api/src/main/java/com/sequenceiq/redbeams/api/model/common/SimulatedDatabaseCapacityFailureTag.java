package com.sequenceiq.redbeams.api.model.common;

/**
 * Database server tag used by E2E tests to simulate capacity failures of the database instance-type attempts, honored only for accounts
 * having the {@code CDP_SIMULATE_DATABASE_CAPACITY_FAILURE} entitlement. Its value is the number of leading instance-type attempts to treat
 * as a capacity failure, or {@value #ALL_ATTEMPTS} to fail every attempt.
 */
public final class SimulatedDatabaseCapacityFailureTag {

    public static final String KEY = "cdp-e2e-simulated-db-capacity-failures";

    public static final String ALL_ATTEMPTS = "all";

    private SimulatedDatabaseCapacityFailureTag() {
    }
}
