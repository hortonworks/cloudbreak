package com.sequenceiq.environment.api.doc.environment;

public class EnvironmentDescription {
    public static final String ENVIRONMENT_NOTES = "Environment consists of a credential and various other resources and enables users to quickly "
            + "create clusters in given regions in a given cloud provider.";

    public static final String TAG_UPDATE_PERMISSIONS_NOTES = "Reports whether the environment's credential is allowed to propagate tag changes to the "
            + "already created cloud resources, so the result can be shown before tag propagation is enabled. Read-only: nothing is modified and no flow "
            + "is started. Always returns 200; an empty response list means every required permission is granted. A non-empty list contains one entry per "
            + "missing permission with code 404, or a single entry with code 503 when the check itself could not be performed (for example the credential "
            + "is not allowed to read its own permissions) and therefore no individual permission can be named.";

    public static final String MAINTENANCE_WINDOW_SCHEDULE_CREATE_EDIT_NOTES =
            "Optional maintenanceWindowSchedule on create sets an ENVIRONMENT-scoped maintenance window override for this environment "
                    + "(after the environment CRN exists). On edit, maintenanceWindowSchedule updates that override, or clearMaintenanceWindowSchedule=true "
                    + "removes it; those fields cannot be combined in the same edit. Any create that includes maintenanceWindowSchedule, or any edit that "
                    + "includes maintenanceWindowSchedule or sets clearMaintenanceWindowSchedule=true, requires the Maintenance Window service to be available "
                    + "in this region/deployment; otherwise the request is rejected with 400. Skip-next and cancel-skip use the Maintenance Window schedule "
                    + "APIs (not Environment create/edit).";

    public static final String MAINTENANCE_WINDOW_SCHEDULE_DESCRIBE_NOTES =
            "Describe may include maintenanceWindowSchedule on DetailedEnvironmentV1Response when the Maintenance Window service returns an "
                    + "ENVIRONMENT-scoped override. The field is omitted when no override exists or when maintenance is unavailable or errors "
                    + "(non-404 failures are not surfaced on describe; the schedule field is left out so describe still succeeds). A missing "
                    + "maintenanceWindowSchedule field does not prove that no override exists — for example Console must not treat absent as "
                    + "\"no schedule\" without confirming maintenance health or querying the Maintenance Window schedule API.";

    private EnvironmentDescription() {
    }
}
