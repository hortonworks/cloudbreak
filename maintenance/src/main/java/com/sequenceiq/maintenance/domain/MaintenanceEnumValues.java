package com.sequenceiq.maintenance.domain;

import java.time.DayOfWeek;
import java.util.Locale;

import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

/**
 * Converts API string values to maintenance enums after {@code @OneOfEnum} validation.
 */
public final class MaintenanceEnumValues {

    private MaintenanceEnumValues() {
    }

    public static MaintenanceScopeType toScopeType(String value) {
        return value == null ? null : MaintenanceScopeType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static MaintenanceRecurrenceKind toRecurrenceKind(String value) {
        return value == null ? null : MaintenanceRecurrenceKind.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static MaintenanceTaskKind toTaskKind(String value) {
        return value == null ? null : MaintenanceTaskKind.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static MaintenanceTaskStatus toTaskStatus(String value) {
        return value == null ? null : MaintenanceTaskStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static DayOfWeek toDayOfWeek(String value) {
        return value == null ? null : DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
