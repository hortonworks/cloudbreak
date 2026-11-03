package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Environment-scoped maintenance window override returned on describe. Subset of the maintenance service schedule model, stable for the public Environment API.
 */
@JsonInclude(Include.NON_NULL)
@Schema(name = "EnvironmentMaintenanceWindowScheduleV1Response")
public class EnvironmentMaintenanceWindowScheduleResponse {

    @Schema(description = EnvironmentModelDescription.MAINTENANCE_WINDOW_SCHEDULE_NAME)
    private String name;

    @Schema(description = EnvironmentModelDescription.MAINTENANCE_WINDOW_SCHEDULE_RECURRENCE_KIND)
    private String recurrenceKind;

    @Schema(description = EnvironmentModelDescription.MAINTENANCE_WINDOW_SCHEDULE_TIMEZONE)
    private String timezone;

    @Schema(description = EnvironmentModelDescription.MAINTENANCE_WINDOW_SCHEDULE_DESCRIPTION)
    private String description;

    @Schema(description = EnvironmentModelDescription.MAINTENANCE_WINDOW_SCHEDULE_DURATION_MINUTES)
    private Integer durationMinutes;

    @Schema(description = EnvironmentModelDescription.MAINTENANCE_WINDOW_SCHEDULE_START_LOCAL_TIME)
    private String startLocalTime;

    @Schema(description = EnvironmentModelDescription.MAINTENANCE_WINDOW_SCHEDULE_DAY_OF_WEEK)
    private String dayOfWeek;

    @Schema(description = EnvironmentModelDescription.MAINTENANCE_WINDOW_SCHEDULE_WEEK_ORDINAL)
    private Integer weekOrdinal;

    @Schema(description = EnvironmentModelDescription.MAINTENANCE_WINDOW_SCHEDULE_DAY_OF_MONTH)
    private Integer dayOfMonth;

    @Schema(description = EnvironmentModelDescription.MAINTENANCE_WINDOW_SCHEDULE_CRON_EXPRESSION)
    private String cronExpression;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRecurrenceKind() {
        return recurrenceKind;
    }

    public void setRecurrenceKind(String recurrenceKind) {
        this.recurrenceKind = recurrenceKind;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getStartLocalTime() {
        return startLocalTime;
    }

    public void setStartLocalTime(String startLocalTime) {
        this.startLocalTime = startLocalTime;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getWeekOrdinal() {
        return weekOrdinal;
    }

    public void setWeekOrdinal(Integer weekOrdinal) {
        this.weekOrdinal = weekOrdinal;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' +
                "name='" + name + '\'' +
                ", recurrenceKind='" + recurrenceKind + '\'' +
                ", timezone='" + timezone + '\'' +
                ", description='" + description + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", startLocalTime='" + startLocalTime + '\'' +
                ", dayOfWeek='" + dayOfWeek + '\'' +
                ", weekOrdinal=" + weekOrdinal +
                ", dayOfMonth=" + dayOfMonth +
                ", cronExpression='" + cronExpression + '\'' +
                '}';
    }
}
