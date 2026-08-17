package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import java.time.DayOfWeek;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.maintenance.api.doc.ModelDescriptions;
import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.ScheduleFields.REQUEST)
public abstract class MaintenanceWindowScheduleFieldsRequest {

    @Size(max = 255)
    @Schema(description = ModelDescriptions.ScheduleFields.NAME)
    private String name;

    @OneOfEnum(enumClass = MaintenanceRecurrenceKind.class, message = "Value must be one of the followings %s", fieldName = "recurrenceKind")
    @Schema(description = ModelDescriptions.ScheduleFields.RECURRENCE_KIND)
    private String recurrenceKind;

    @Schema(description = ModelDescriptions.ScheduleFields.TIMEZONE)
    private String timezone;

    @Size(max = 1024)
    @Schema(description = ModelDescriptions.ScheduleFields.DESCRIPTION)
    private String description;

    @Min(60)
    @Schema(description = ModelDescriptions.ScheduleFields.DURATION_MINUTES)
    private Integer durationMinutes;

    @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "startLocalTime must be in HH:mm format")
    @Schema(description = ModelDescriptions.ScheduleFields.START_LOCAL_TIME)
    private String startLocalTime;

    @OneOfEnum(enumClass = DayOfWeek.class, message = "Value must be one of the followings %s", fieldName = "dayOfWeek")
    @Schema(description = ModelDescriptions.ScheduleFields.DAY_OF_WEEK)
    private String dayOfWeek;

    @Min(1)
    @Max(5)
    @Schema(description = ModelDescriptions.ScheduleFields.WEEK_ORDINAL)
    private Integer weekOrdinal;

    @Min(1)
    @Max(31)
    @Schema(description = ModelDescriptions.ScheduleFields.DAY_OF_MONTH)
    private Integer dayOfMonth;

    @Schema(description = ModelDescriptions.ScheduleFields.CRON_EXPRESSION)
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
