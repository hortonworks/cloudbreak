package com.sequenceiq.environment.maintenancewindow;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentMaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleFieldsRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRestoreRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;

public final class EnvironmentMaintenanceWindowScheduleMapper {

    private EnvironmentMaintenanceWindowScheduleMapper() {
    }

    public static MaintenanceWindowScheduleRequest toCreateRequest(
            MaintenanceWindowScheduleFieldsRequest source, String environmentCrn) {
        MaintenanceWindowScheduleRequest target = new MaintenanceWindowScheduleRequest();
        copyScheduleFields(source, target);
        target.setScopeType(MaintenanceScopeType.ENVIRONMENT.name());
        target.setScopeId(environmentCrn);
        return target;
    }

    public static UpdateMaintenanceWindowScheduleRequest toUpdateRequest(MaintenanceWindowScheduleFieldsRequest source) {
        UpdateMaintenanceWindowScheduleRequest target = new UpdateMaintenanceWindowScheduleRequest();
        copyScheduleFields(source, target);
        return target;
    }

    /**
     * Builds a full-state restore request from a pre-edit snapshot. Carries the schedule id so the maintenance service
     * reactivates the same row (preserving its id and attached skips) and overwrites every field, including nulls.
     */
    public static MaintenanceWindowScheduleRestoreRequest toRestoreRequest(
            MaintenanceWindowScheduleResponse source, String environmentCrn) {
        MaintenanceWindowScheduleRestoreRequest target = new MaintenanceWindowScheduleRestoreRequest();
        copyScheduleFields(source, target);
        target.setScopeType(MaintenanceScopeType.ENVIRONMENT.name());
        target.setScopeId(environmentCrn);
        target.setId(source.getId());
        return target;
    }

    public static EnvironmentMaintenanceWindowScheduleResponse toEnvironmentResponse(MaintenanceWindowScheduleResponse source) {
        EnvironmentMaintenanceWindowScheduleResponse target = new EnvironmentMaintenanceWindowScheduleResponse();
        target.setName(source.getName());
        target.setRecurrenceKind(source.getRecurrenceKind());
        target.setTimezone(source.getTimezone());
        target.setDescription(source.getDescription());
        target.setDurationMinutes(source.getDurationMinutes());
        target.setStartLocalTime(source.getStartLocalTime());
        target.setDayOfWeek(source.getDayOfWeek());
        target.setWeekOrdinal(source.getWeekOrdinal());
        target.setDayOfMonth(source.getDayOfMonth());
        target.setCronExpression(source.getCronExpression());
        return target;
    }

    private static void copyScheduleFields(
            MaintenanceWindowScheduleFieldsRequest source, MaintenanceWindowScheduleFieldsRequest target) {
        target.setName(source.getName());
        target.setRecurrenceKind(source.getRecurrenceKind());
        target.setTimezone(source.getTimezone());
        target.setDescription(source.getDescription());
        target.setDurationMinutes(source.getDurationMinutes());
        target.setStartLocalTime(source.getStartLocalTime());
        target.setDayOfWeek(source.getDayOfWeek());
        target.setWeekOrdinal(source.getWeekOrdinal());
        target.setDayOfMonth(source.getDayOfMonth());
        target.setCronExpression(source.getCronExpression());
    }

    private static void copyScheduleFields(
            MaintenanceWindowScheduleResponse source, MaintenanceWindowScheduleFieldsRequest target) {
        target.setName(source.getName());
        target.setRecurrenceKind(source.getRecurrenceKind());
        target.setTimezone(source.getTimezone());
        target.setDescription(source.getDescription());
        target.setDurationMinutes(source.getDurationMinutes());
        target.setStartLocalTime(source.getStartLocalTime());
        target.setDayOfWeek(source.getDayOfWeek());
        target.setWeekOrdinal(source.getWeekOrdinal());
        target.setDayOfMonth(source.getDayOfMonth());
        target.setCronExpression(source.getCronExpression());
    }
}
