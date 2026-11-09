package com.sequenceiq.maintenance.api.v1.schedule.validation;

/**
 * User-facing validation messages shared by the services that accept a maintenance window schedule on create (Data Hub,
 * Data Lake, FreeIPA, ...). Lives in maintenance-api so every consumer rejects an unavailable/unentitled schedule with
 * the same wording, rather than each module keeping its own copy next to its Spring {@code @ConditionalOnExpression}
 * strings.
 */
public final class MaintenanceWindowValidationMessages {

    public static final String NOT_CONFIGURED_VALIDATION_ERROR =
            "Maintenance window scheduling is not available in this environment. Please remove the maintenance window "
                    + "schedule from your request, or contact your administrator if you need this feature enabled.";

    public static final String NOT_ENTITLED_VALIDATION_ERROR =
            "Maintenance window scheduling is not enabled for your account. Please remove the maintenance window "
                    + "schedule from your request, or contact your administrator to request access.";

    private MaintenanceWindowValidationMessages() {
    }
}
