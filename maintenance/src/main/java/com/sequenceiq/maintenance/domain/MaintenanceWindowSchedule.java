package com.sequenceiq.maintenance.domain;

import java.io.Serializable;
import java.time.DayOfWeek;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.sequenceiq.maintenance.domain.converter.DayOfWeekConverter;
import com.sequenceiq.maintenance.domain.converter.MaintenanceRecurrenceKindConverter;
import com.sequenceiq.maintenance.domain.converter.MaintenanceScopeTypeConverter;

@Entity
@Table(name = "maintenance_window_schedule")
public class MaintenanceWindowSchedule implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "maintenance_window_schedule_generator")
    @SequenceGenerator(name = "maintenance_window_schedule_generator", sequenceName = "maintenance_window_schedule_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String name;

    @Convert(converter = MaintenanceScopeTypeConverter.class)
    @Column(name = "scope_type", nullable = false)
    private MaintenanceScopeType scopeType;

    @Column(name = "scope_id", nullable = false)
    private String scopeId;

    @Convert(converter = MaintenanceRecurrenceKindConverter.class)
    @Column(name = "recurrence_kind", nullable = false)
    private MaintenanceRecurrenceKind recurrenceKind;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "start_local_time")
    private String startLocalTime;

    @Convert(converter = DayOfWeekConverter.class)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    @Column(name = "week_ordinal")
    private Integer weekOrdinal;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "cron_dialect")
    private String cronDialect = "QUARTZ";

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(nullable = false)
    private boolean archived;

    @Version
    @Column(nullable = false, insertable = false)
    private Integer version;

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

    public MaintenanceScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(MaintenanceScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public MaintenanceRecurrenceKind getRecurrenceKind() {
        return recurrenceKind;
    }

    public void setRecurrenceKind(MaintenanceRecurrenceKind recurrenceKind) {
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

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
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

    public String getCronDialect() {
        return cronDialect;
    }

    public void setCronDialect(String cronDialect) {
        this.cronDialect = cronDialect;
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

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowSchedule{" +
                "id=" + id +
                ", accountId='" + accountId + '\'' +
                ", name='" + name + '\'' +
                ", scopeType=" + scopeType +
                ", scopeId='" + scopeId + '\'' +
                ", recurrenceKind=" + recurrenceKind +
                ", timezone='" + timezone + '\'' +
                ", description='" + description + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", startLocalTime='" + startLocalTime + '\'' +
                ", dayOfWeek=" + dayOfWeek +
                ", weekOrdinal=" + weekOrdinal +
                ", dayOfMonth=" + dayOfMonth +
                ", cronDialect='" + cronDialect + '\'' +
                ", cronExpression='" + cronExpression + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", createdBy='" + createdBy + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                ", archived=" + archived +
                ", version=" + version +
                '}';
    }
}
