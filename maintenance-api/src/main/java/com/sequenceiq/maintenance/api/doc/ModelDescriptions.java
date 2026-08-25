package com.sequenceiq.maintenance.api.doc;

public final class ModelDescriptions {

    public static final String EPOCH_MS = "Unix epoch timestamp in milliseconds (UTC)";

    private ModelDescriptions() {
    }

    public static final class ScheduleFields {

        public static final String REQUEST = "Mutable maintenance window schedule fields shared by create and update requests";

        public static final String NAME = "Display name; defaulted from scope when omitted on create";

        public static final String RECURRENCE_KIND = "Recurrence pattern kind";

        public static final String TIMEZONE = "IANA timezone for local schedule fields; defaults to UTC on create when omitted";

        public static final String DESCRIPTION = "Optional human-readable description";

        public static final String DURATION_MINUTES = "Maintenance window duration in minutes (minimum 60)";

        public static final String START_LOCAL_TIME = "Local start time in HH:mm (24-hour)";

        public static final String DAY_OF_WEEK = "Day of week for WEEKLY and MONTHLY_NTH_WEEKDAY schedules";

        public static final String WEEK_ORDINAL = "Nth weekday of month (1-5) for MONTHLY_NTH_WEEKDAY";

        public static final String DAY_OF_MONTH = "Day of month (1-31) for MONTHLY_DAY_OF_MONTH";

        public static final String CRON_EXPRESSION = "Quartz cron expression for CRON schedules";

        private ScheduleFields() {
        }
    }

    public static final class ScheduleRequest {

        public static final String REQUEST = "Request to create a maintenance window schedule for a scope";

        public static final String SCOPE_TYPE = "Scope type";

        public static final String SCOPE_ID = "Scope identifier (account id for TENANT, resource CRN for other scopes)";

        private ScheduleRequest() {
        }
    }

    public static final class ScheduleUpdateRequest {

        public static final String REQUEST = "Partial update of mutable schedule fields; only non-null fields are applied."
                + " When recurrenceKind is present, all fields required for that pattern must be included in the same request"
                + " and fields for other patterns must be omitted.";

        public static final String TIMEZONE = "IANA timezone for local schedule fields";

        private ScheduleUpdateRequest() {
        }
    }

    public static final class ScheduleListParams {

        public static final String PARAMS = "Optional scope filter query parameters for listing schedules";

        public static final String SCOPE_TYPE = "Filter by scope type; must be provided together with scopeId";

        public static final String SCOPE_ID = "Filter by scope id; must be provided together with scopeType";

        private ScheduleListParams() {
        }
    }

    public static final class ScheduleResponse {

        public static final String RESPONSE = "Maintenance window schedule with computed upcoming occurrences";

        public static final String ID = "Internal schedule id";

        public static final String ACCOUNT_ID = "Account id derived from authentication";

        public static final String NAME = "Schedule display name";

        public static final String SCOPE_TYPE = "Scope type";

        public static final String SCOPE_ID = "Scope identifier";

        public static final String RECURRENCE_KIND = "Recurrence pattern kind";

        public static final String TIMEZONE = "IANA timezone";

        public static final String DESCRIPTION = "Optional description";

        public static final String DURATION_MINUTES = "Window duration in minutes";

        public static final String START_LOCAL_TIME = "Local start time in HH:mm";

        public static final String DAY_OF_WEEK = "Day of week";

        public static final String WEEK_ORDINAL = "Nth weekday of month (1-5)";

        public static final String DAY_OF_MONTH = "Day of month (1-31)";

        public static final String CRON_EXPRESSION = "Quartz cron expression when recurrenceKind is CRON";

        public static final String CREATED_AT = EPOCH_MS;

        public static final String UPDATED_AT = EPOCH_MS;

        public static final String CREATED_BY = "Creator user CRN";

        public static final String UPDATED_BY = "Last updater user CRN";

        public static final String VERSION = "Optimistic lock version";

        public static final String RECURRENCE_SUMMARY = "Server-generated display hint in English (Locale.ROOT). "
                + "Structured fields such as recurrenceKind, dayOfWeek, and cronExpression are authoritative; "
                + "for CRON schedules use cronExpression rather than this summary.";

        public static final String NEXT_OCCURRENCE_START = "Start of the next upcoming occurrence (" + EPOCH_MS + ")";

        public static final String NEXT_OCCURRENCE_END = "End of the next upcoming occurrence (" + EPOCH_MS + ")";

        public static final String UPCOMING_OCCURRENCES = "Next few upcoming window occurrences";

        private ScheduleResponse() {
        }
    }

    public static final class ScheduleListResponse {

        public static final String RESPONSE = "List of maintenance window schedules";

        public static final String SCHEDULES = "Matching schedules for the authenticated account";

        private ScheduleListResponse() {
        }
    }

    public static final class SkipRequest {

        public static final String REQUEST = "Optional metadata for skipping the next maintenance window occurrence";

        public static final String REASON = "Optional reason recorded with the skip";

        private SkipRequest() {
        }
    }

    public static final class SkipResponse {

        public static final String RESPONSE = "Recorded skip for a specific maintenance window occurrence";

        public static final String ID = "Skip record id";

        public static final String MAINTENANCE_WINDOW_SCHEDULE_ID = "Parent schedule id";

        public static final String WINDOW_START = "Skipped window start (" + EPOCH_MS + ")";

        public static final String WINDOW_END = "Skipped window end (" + EPOCH_MS + ")";

        public static final String TIMEZONE = "Schedule timezone at time of skip";

        public static final String CREATED_AT = EPOCH_MS;

        public static final String CREATED_BY = "Creator user CRN";

        public static final String REASON = "Optional skip reason";

        private SkipResponse() {
        }
    }

    public static final class Occurrence {

        public static final String RESPONSE = "Single computed maintenance window occurrence";

        public static final String WINDOW_START = "Window start (" + EPOCH_MS + ")";

        public static final String WINDOW_END = "Window end (" + EPOCH_MS + ")";

        private Occurrence() {
        }
    }

    public static final class TaskRequest {

        public static final String REQUEST = "Request to register a maintenance window task for gated dispatch";

        public static final String RESOURCE_CRN = "Target resource CRN (Data Hub, Data Lake, or FreeIPA)";

        public static final String ENVIRONMENT_CRN = "Environment CRN for schedule scope resolution";

        public static final String TASK_TYPE = "Submitter-defined task type (for example secret-rotation)";

        public static final String WORK_ITEM_ID = "Submitter-defined work item id (for example secret id or runtime version)";

        public static final String TASK_KIND = "EVERY_WINDOW runs on each matching window; ONE_SHOT completes after first dispatch";

        public static final String SUBMITTER_SERVICE = "Calling service name";

        public static final String TASK_PAYLOAD = "Optional opaque payload passed to the executor";

        public static final String EXECUTION_REF = "Executor reference (HTTP callback, Quartz job descriptor, etc.)";

        public static final String PRIORITY = "Dispatcher priority (higher runs first; default 100)";

        public static final String DEPENDS_ON = "Optional ACTIVE task that must complete for the same window occurrence before dispatch";

        public static final String RETRY_WITHIN_OCCURRENCE = "Whether failed runs may retry within the same window";

        public static final String MAX_ATTEMPTS_PER_OCCURRENCE = "Maximum attempts per window occurrence (platform-capped)";

        public static final String RETRY_COOLDOWN_MINUTES = "Minimum minutes between retries within the same occurrence";

        private TaskRequest() {
        }
    }

    public static final class TaskUpdateRequest {

        public static final String REQUEST = "Partial update of a maintenance window task";

        public static final String STATUS = "Task status; set DISABLED to remove from the dispatcher set without deleting";

        public static final String PRIORITY = TaskRequest.PRIORITY;

        public static final String DEPENDS_ON = TaskRequest.DEPENDS_ON;

        public static final String RETRY_WITHIN_OCCURRENCE = TaskRequest.RETRY_WITHIN_OCCURRENCE;

        public static final String MAX_ATTEMPTS_PER_OCCURRENCE = TaskRequest.MAX_ATTEMPTS_PER_OCCURRENCE;

        public static final String RETRY_COOLDOWN_MINUTES = TaskRequest.RETRY_COOLDOWN_MINUTES;

        public static final String TASK_PAYLOAD = TaskRequest.TASK_PAYLOAD;

        public static final String EXECUTION_REF = TaskRequest.EXECUTION_REF;

        private TaskUpdateRequest() {
        }
    }

    public static final class TaskListParams {

        public static final String PARAMS = "Optional filters for listing maintenance window tasks";

        public static final String RESOURCE_CRN = "Filter by resource CRN";

        public static final String ENVIRONMENT_CRN = "Filter by environment CRN";

        public static final String TASK_TYPE = "Filter by task type; requires resourceCrn when set";

        public static final String WORK_ITEM_ID = "Filter by work item id; requires resourceCrn and taskType when set";

        public static final String TASK_KIND = "Filter by task kind (EVERY_WINDOW or ONE_SHOT)";

        public static final String STATUS = "Filter by task status";

        private TaskListParams() {
        }
    }

    public static final class TaskResponse {

        public static final String RESPONSE = "Registered maintenance window task";

        public static final String ID = "Task id";

        public static final String ACCOUNT_ID = "Account id";

        public static final String RESOURCE_CRN = TaskRequest.RESOURCE_CRN;

        public static final String ENVIRONMENT_CRN = TaskRequest.ENVIRONMENT_CRN;

        public static final String TASK_TYPE = TaskRequest.TASK_TYPE;

        public static final String WORK_ITEM_ID = TaskRequest.WORK_ITEM_ID;

        public static final String TASK_KIND = TaskRequest.TASK_KIND;

        public static final String STATUS = "Task status";

        public static final String SUBMITTER_SERVICE = TaskRequest.SUBMITTER_SERVICE;

        public static final String TASK_PAYLOAD = TaskRequest.TASK_PAYLOAD;

        public static final String EXECUTION_REF = TaskRequest.EXECUTION_REF;

        public static final String PRIORITY = TaskRequest.PRIORITY;

        public static final String DEPENDS_ON = TaskRequest.DEPENDS_ON;

        public static final String RETRY_WITHIN_OCCURRENCE = TaskRequest.RETRY_WITHIN_OCCURRENCE;

        public static final String MAX_ATTEMPTS_PER_OCCURRENCE = TaskRequest.MAX_ATTEMPTS_PER_OCCURRENCE;

        public static final String RETRY_COOLDOWN_MINUTES = TaskRequest.RETRY_COOLDOWN_MINUTES;

        public static final String CREATED_AT = EPOCH_MS;

        public static final String UPDATED_AT = EPOCH_MS;

        public static final String CREATED_BY = "Creator identity";

        public static final String UPDATED_BY = "Last updater identity";

        public static final String DISABLED_AT = "When the task was disabled (" + EPOCH_MS + ")";

        public static final String COMPLETED_AT = "When the task completed (" + EPOCH_MS + ")";

        public static final String VERSION = "Optimistic lock version";

        private TaskResponse() {
        }
    }

    public static final class TaskListResponse {

        public static final String RESPONSE = "List of maintenance window tasks";

        public static final String TASKS = "Matching tasks for the authenticated account";

        private TaskListResponse() {
        }
    }

    public static final class TaskDependency {

        public static final String REQUEST = "Reference to another registered task by its natural key";

        public static final String RESPONSE = "Registered task referenced as a dependency";

        public static final String RESOURCE_CRN = "Dependency task resource CRN";

        public static final String TASK_TYPE = "Dependency task type";

        public static final String WORK_ITEM_ID = "Dependency work item id";

        private TaskDependency() {
        }
    }
}
