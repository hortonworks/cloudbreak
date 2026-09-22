package com.sequenceiq.maintenance.api.doc;

public final class MaintenanceWindowScheduleInternalOpDescription {

    public static final String TAG = "/internal/maintenance-schedules";

    public static final String TAG_DESCRIPTION = "Internal maintenance window schedule lookups for service-to-service callers";

    public static final String GET = "Get a maintenance window schedule (internal)";

    public static final String GET_NOTES = "Account scope is supplied explicitly via accountId for internal actors.";

    public static final String SCOPE_TYPE = MaintenanceWindowScheduleOpDescription.SCOPE_TYPE;

    public static final String SCOPE_ID = MaintenanceWindowScheduleOpDescription.SCOPE_ID;

    private MaintenanceWindowScheduleInternalOpDescription() {
    }
}
