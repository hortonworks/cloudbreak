package com.sequenceiq.maintenance.api.doc;

public final class MaintenanceWindowScheduleInternalOpDescription {

    public static final String TAG = "/internal/maintenance-schedules";

    public static final String TAG_DESCRIPTION = "Internal maintenance window schedule lookups for service-to-service callers";

    public static final String GET = "Get a maintenance window schedule (internal)";

    public static final String GET_NOTES = "Account scope is supplied explicitly via accountId for internal actors.";

    public static final String RESTORE = "Restore a maintenance window schedule to a prior state (internal)";

    public static final String RESTORE_NOTES = "Reactivates the schedule row identified by id (preserving its id and attached skips) "
            + "and replaces every mutable field with the supplied values, including nulls. Used to compensate a failed cross-service "
            + "edit. Account scope is supplied explicitly via accountId for internal actors.";

    public static final String CREATE = "Create a maintenance window schedule (internal)";

    public static final String CREATE_NOTES = "Service-to-service create: the calling service has already authorized the user for the "
            + "scoped resource operation, so no per-user authorization is performed here. Account scope is supplied explicitly via accountId.";

    public static final String DELETE = "Delete a maintenance window schedule (internal)";

    public static final String DELETE_NOTES = "Service-to-service delete: authorization is delegated to the calling service, so the schedule "
            + "can be cleaned up during resource deletion regardless of the triggering user's current entitlement or roles. "
            + "Account scope is supplied explicitly via accountId.";

    public static final String SCOPE_TYPE = MaintenanceWindowScheduleOpDescription.SCOPE_TYPE;

    public static final String SCOPE_ID = MaintenanceWindowScheduleOpDescription.SCOPE_ID;

    public static final String INITIATOR_USER_CRN = "CRN of the end user who initiated the owning resource operation; recorded as "
            + "createdBy/updatedBy so audit reflects the real user rather than the internal actor the call runs as.";

    private MaintenanceWindowScheduleInternalOpDescription() {
    }
}
