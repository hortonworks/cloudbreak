package com.sequenceiq.maintenance.api.doc;

public final class MaintenanceWindowScheduleOpDescription {

    public static final String TAG = "/v1/maintenance/schedules";

    public static final String TAG_DESCRIPTION = "Maintenance window schedules and skip rules for the authenticated account";

    public static final String NOTES = "Account scope is derived from the authenticated user CRN. "
            + "Schedules are keyed by scopeType and scopeId (for example TENANT/accountId, ENVIRONMENT/environmentCrn).";

    public static final String AUTH_NOTES = "Authorization is as follows - TENANT schedules: read requires power user or "
            + "environment creator; write requires power user (tenant admin). ENVIRONMENT schedules: describe/edit environment on "
            + "scopeId. DATAHUB/DATALAKE schedules: describe/write on the resource CRN (datahub/write, modifyDatalake). "
            + "FREEIPA schedules: repair/admin/describe environment rights on scopeId. Internal task/register/run APIs are "
            + "@InternalOnly (service identity). See maintenance-api AUTH.md for the full matrix.";

    public static final String SCOPE_TYPE = "Scope type (TENANT, ENVIRONMENT, DATAHUB, DATALAKE, FREEIPA)";

    public static final String SCOPE_ID = "Scope identifier (account id for TENANT, resource CRN for other scopes)";

    public static final String LIST = "List maintenance window schedules";

    public static final String PREVIEW = "Preview upcoming maintenance window occurrences";

    public static final String PREVIEW_NOTES = NOTES
            + " Computes occurrences server-side from the request body without persisting a schedule."
            + " The body must include the full schedule field set required for create (recurrenceKind, durationMinutes,"
            + " and pattern-specific fields); clients simulating an update must merge the persisted schedule with edits"
            + " before calling preview. Optional scopeType and scopeId validate scope and load persisted skip-next marks;"
            + " overlap/gap rules apply to the request body only (same as PATCH), not to other windows on the stored schedule.";

    public static final String EFFECTIVE = "Get the effective maintenance window schedule for a resource";

    public static final String EFFECTIVE_NOTES = NOTES
            + " Resolves inheritance in order: resource scope, then parent environment when the owning service knows the"
            + " resource, then tenant. Optional environmentCrn must match that resolved parent environment when provided."
            + " Returns the winning schedule plus sourceScopeType and sourceScopeId indicating where it is defined.";

    public static final String GET = "Get a maintenance window schedule";

    public static final String CREATE = "Create a maintenance window schedule";

    public static final String UPDATE = "Update a maintenance window schedule";

    public static final String UPDATE_NOTES = NOTES
            + " Partial update: only non-null fields in the request body are applied."
            + " If recurrenceKind is included, the request must also supply the full field set required for that pattern"
            + " in the same body (for example WEEKLY requires startLocalTime and dayOfWeek; CRON requires cronExpression),"
            + " and must omit fields used by other patterns."
            + " Omit recurrenceKind to patch other fields without resubmitting the full recurrence configuration.";

    public static final String DELETE = "Delete a maintenance window schedule";

    public static final String SKIP = "Skip the next maintenance window occurrence";

    public static final String SKIP_NOTES = NOTES
            + " Skips the earliest occurrence with windowEnd > now that has not yet started (windowStart > now).";

    public static final String CANCEL_SKIP = "Cancel a skip for the current or next maintenance window";

    public static final String CANCEL_SKIP_NOTES = NOTES
            + " Cancels the skip covering the current time, or the earliest not-yet-ended skip.";

    public static final String LOCATION =
            "URI of the created schedule: /v1/maintenance/schedules/scope/{scopeType}/scopeId/{scopeId}";

    private MaintenanceWindowScheduleOpDescription() {
    }
}
