package com.sequenceiq.maintenance.client.schedule;

import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleFieldsRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;

public final class MaintenanceWindowScheduleCreateMapper {

    private MaintenanceWindowScheduleCreateMapper() {
    }

    public static MaintenanceWindowScheduleRequest toCreateRequest(
            MaintenanceWindowScheduleFieldsRequest source, MaintenanceScopeType scopeType, String scopeId) {
        MaintenanceWindowScheduleRequest target = new MaintenanceWindowScheduleRequest();
        copyScheduleFields(source, target);
        target.setScopeType(scopeType.name());
        target.setScopeId(scopeId);
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
}
