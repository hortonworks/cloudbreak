package com.sequenceiq.maintenance.api.doc;

public final class MaintenanceWindowTaskOpDescription {

    public static final String TAG = "/internal/maintenance-tasks";

    public static final String TAG_DESCRIPTION = "Internal maintenance window task registration for submitter services";

    public static final String NOTES = "Account scope is derived from the authenticated caller. "
            + "At most one ACTIVE task may exist per (resourceCrn, taskType, workItemId); "
            + "re-registration returns the existing ACTIVE row when the request payload matches, otherwise 409 Conflict. "
            + "On re-registration, omitted optional fields (priority, retry settings) are not compared against the stored task.";

    public static final String TASK_ID = "Maintenance window task id";

    public static final String LIST = "List maintenance window tasks";

    public static final String LIST_NOTES = NOTES + " Expected cardinality is O(tens) per account; "
            + "production callers should filter by resourceCrn, environmentCrn, or status=ACTIVE.";

    public static final String GET = "Get a maintenance window task";

    public static final String CREATE = "Register a maintenance window task";

    public static final String UPDATE = "Update a maintenance window task";

    public static final String UPDATE_NOTES = NOTES + " Partial update: only non-null fields in the request body are applied.";

    public static final String DELETE = "Delete a maintenance window task";

    public static final String DELETE_NOTES = NOTES + " Soft-deletes the task (status DELETED), removing it from the dispatcher set.";

    public static final String LOCATION = "URI of the created task: /internal/maintenance-tasks/{taskId}";

    private MaintenanceWindowTaskOpDescription() {
    }
}
