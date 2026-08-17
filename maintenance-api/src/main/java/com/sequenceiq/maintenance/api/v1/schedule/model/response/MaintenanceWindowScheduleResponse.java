package com.sequenceiq.maintenance.api.v1.schedule.model.response;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.maintenance.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.ScheduleResponse.RESPONSE)
public class MaintenanceWindowScheduleResponse {

    @Schema(description = ModelDescriptions.ScheduleResponse.ID)
    private Long id;

    @Schema(description = ModelDescriptions.ScheduleResponse.ACCOUNT_ID)
    private String accountId;

    @Schema(description = ModelDescriptions.ScheduleResponse.NAME)
    private String name;

    @Schema(description = ModelDescriptions.ScheduleResponse.SCOPE_TYPE)
    private String scopeType;

    @Schema(description = ModelDescriptions.ScheduleResponse.SCOPE_ID)
    private String scopeId;

    @Schema(description = ModelDescriptions.ScheduleResponse.RECURRENCE_KIND)
    private String recurrenceKind;

    @Schema(description = ModelDescriptions.ScheduleResponse.TIMEZONE)
    private String timezone;

    @Schema(description = ModelDescriptions.ScheduleResponse.DESCRIPTION)
    private String description;

    @Schema(description = ModelDescriptions.ScheduleResponse.DURATION_MINUTES)
    private Integer durationMinutes;

    @Schema(description = ModelDescriptions.ScheduleResponse.START_LOCAL_TIME)
    private String startLocalTime;

    @Schema(description = ModelDescriptions.ScheduleResponse.DAY_OF_WEEK)
    private String dayOfWeek;

    @Schema(description = ModelDescriptions.ScheduleResponse.WEEK_ORDINAL)
    private Integer weekOrdinal;

    @Schema(description = ModelDescriptions.ScheduleResponse.DAY_OF_MONTH)
    private Integer dayOfMonth;

    @Schema(description = ModelDescriptions.ScheduleResponse.CRON_EXPRESSION)
    private String cronExpression;

    @Schema(description = ModelDescriptions.ScheduleResponse.CREATED_AT)
    private Long createdAt;

    @Schema(description = ModelDescriptions.ScheduleResponse.UPDATED_AT)
    private Long updatedAt;

    @Schema(description = ModelDescriptions.ScheduleResponse.CREATED_BY)
    private String createdBy;

    @Schema(description = ModelDescriptions.ScheduleResponse.UPDATED_BY)
    private String updatedBy;

    @Schema(description = ModelDescriptions.ScheduleResponse.VERSION)
    private Integer version;

    @Schema(description = ModelDescriptions.ScheduleResponse.RECURRENCE_SUMMARY)
    private String recurrenceSummary;

    @Schema(description = ModelDescriptions.ScheduleResponse.NEXT_OCCURRENCE_START)
    private Long nextOccurrenceStart;

    @Schema(description = ModelDescriptions.ScheduleResponse.NEXT_OCCURRENCE_END)
    private Long nextOccurrenceEnd;

    private List<MaintenanceWindowOccurrenceResponse> upcomingOccurrences = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getRecurrenceSummary() {
        return recurrenceSummary;
    }

    public void setRecurrenceSummary(String recurrenceSummary) {
        this.recurrenceSummary = recurrenceSummary;
    }

    public Long getNextOccurrenceStart() {
        return nextOccurrenceStart;
    }

    public void setNextOccurrenceStart(Long nextOccurrenceStart) {
        this.nextOccurrenceStart = nextOccurrenceStart;
    }

    public Long getNextOccurrenceEnd() {
        return nextOccurrenceEnd;
    }

    public void setNextOccurrenceEnd(Long nextOccurrenceEnd) {
        this.nextOccurrenceEnd = nextOccurrenceEnd;
    }

    @Schema(description = ModelDescriptions.ScheduleResponse.UPCOMING_OCCURRENCES)
    public List<MaintenanceWindowOccurrenceResponse> getUpcomingOccurrences() {
        return upcomingOccurrences;
    }

    public void setUpcomingOccurrences(List<MaintenanceWindowOccurrenceResponse> upcomingOccurrences) {
        this.upcomingOccurrences = upcomingOccurrences;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowScheduleResponse{" +
                "id=" + id +
                ", accountId='" + accountId + '\'' +
                ", name='" + name + '\'' +
                ", scopeType='" + scopeType + '\'' +
                ", scopeId='" + scopeId + '\'' +
                ", recurrenceKind='" + recurrenceKind + '\'' +
                ", timezone='" + timezone + '\'' +
                ", description='" + description + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", startLocalTime='" + startLocalTime + '\'' +
                ", dayOfWeek='" + dayOfWeek + '\'' +
                ", weekOrdinal=" + weekOrdinal +
                ", dayOfMonth=" + dayOfMonth +
                ", cronExpression='" + cronExpression + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", createdBy='" + createdBy + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", version=" + version +
                ", recurrenceSummary='" + recurrenceSummary + '\'' +
                ", nextOccurrenceStart=" + nextOccurrenceStart +
                ", nextOccurrenceEnd=" + nextOccurrenceEnd +
                ", upcomingOccurrences=" + upcomingOccurrences +
                '}';
    }
}
