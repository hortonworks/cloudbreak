package com.sequenceiq.maintenance.api.v1.schedule.validation;

import java.time.DateTimeException;
import java.time.ZoneId;

import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;

import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleFieldsRequest;

public final class MaintenanceScheduleRecurrenceFieldValidator {

    private MaintenanceScheduleRecurrenceFieldValidator() {
    }

    public static boolean isValidForCreate(MaintenanceWindowScheduleFieldsRequest request) {
        if (!isTimezoneValidWhenPresent(request.getTimezone())) {
            return false;
        }
        if (request.getRecurrenceKind() == null) {
            return false;
        }
        return isRecurrenceConfigurationValid(request, request.getRecurrenceKind());
    }

    public static boolean isValidForPatch(MaintenanceWindowScheduleFieldsRequest request) {
        if (!isTimezoneValidWhenPresent(request.getTimezone())) {
            return false;
        }
        if (request.getRecurrenceKind() == null) {
            return isCronExpressionValidWhenPresent(request.getCronExpression());
        }
        return isRecurrenceConfigurationValid(request, request.getRecurrenceKind());
    }

    private static boolean isRecurrenceConfigurationValid(MaintenanceWindowScheduleFieldsRequest request, String recurrenceKind) {
        MaintenanceRecurrenceKind kind = parseRecurrenceKind(recurrenceKind);
        if (kind == null) {
            return false;
        }
        return switch (kind) {
            case WEEKLY -> isValidWeekly(request);
            case MONTHLY_NTH_WEEKDAY -> isValidMonthlyNthWeekday(request);
            case MONTHLY_DAY_OF_MONTH -> isValidMonthlyDayOfMonth(request);
            case CRON -> isValidCron(request);
        };
    }

    private static MaintenanceRecurrenceKind parseRecurrenceKind(String recurrenceKind) {
        try {
            return MaintenanceRecurrenceKind.valueOf(recurrenceKind);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isValidWeekly(MaintenanceWindowScheduleFieldsRequest request) {
        return hasStartLocalTime(request)
                && request.getDayOfWeek() != null
                && !hasWeekOrdinal(request)
                && !hasDayOfMonth(request)
                && !hasCronExpression(request);
    }

    private static boolean isValidMonthlyNthWeekday(MaintenanceWindowScheduleFieldsRequest request) {
        return hasStartLocalTime(request)
                && request.getDayOfWeek() != null
                && request.getWeekOrdinal() != null
                && !hasDayOfMonth(request)
                && !hasCronExpression(request);
    }

    private static boolean isValidMonthlyDayOfMonth(MaintenanceWindowScheduleFieldsRequest request) {
        return hasStartLocalTime(request)
                && request.getDayOfMonth() != null
                && request.getDayOfWeek() == null
                && !hasWeekOrdinal(request)
                && !hasCronExpression(request);
    }

    private static boolean isValidCron(MaintenanceWindowScheduleFieldsRequest request) {
        return hasCronExpression(request)
                && isValidQuartzCronExpression(request.getCronExpression())
                && !hasStartLocalTime(request)
                && request.getDayOfWeek() == null
                && !hasWeekOrdinal(request)
                && !hasDayOfMonth(request);
    }

    private static boolean isTimezoneValidWhenPresent(String timezone) {
        if (StringUtils.isBlank(timezone)) {
            return true;
        }
        try {
            ZoneId.of(timezone);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static boolean hasStartLocalTime(MaintenanceWindowScheduleFieldsRequest request) {
        return !StringUtils.isBlank(request.getStartLocalTime());
    }

    private static boolean hasWeekOrdinal(MaintenanceWindowScheduleFieldsRequest request) {
        return request.getWeekOrdinal() != null;
    }

    private static boolean hasDayOfMonth(MaintenanceWindowScheduleFieldsRequest request) {
        return request.getDayOfMonth() != null;
    }

    private static boolean hasCronExpression(MaintenanceWindowScheduleFieldsRequest request) {
        return !StringUtils.isBlank(request.getCronExpression());
    }

    private static boolean isCronExpressionValidWhenPresent(String cronExpression) {
        if (StringUtils.isBlank(cronExpression)) {
            return true;
        }
        return isValidQuartzCronExpression(cronExpression);
    }

    private static boolean isValidQuartzCronExpression(String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
    }
}
