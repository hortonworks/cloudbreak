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
}
