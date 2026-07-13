package com.sequenceiq.maintenance.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "maintenance_window_skip")
public class MaintenanceWindowSkip implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "maintenance_window_skip_generator")
    @SequenceGenerator(name = "maintenance_window_skip_generator", sequenceName = "maintenance_window_skip_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_schedule_id", nullable = false)
    private MaintenanceWindowSchedule maintenanceWindowSchedule;

    @Column(name = "window_start", nullable = false)
    private Long windowStart;

    @Column(name = "window_end", nullable = false)
    private Long windowEnd;

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MaintenanceWindowSchedule getMaintenanceWindowSchedule() {
        return maintenanceWindowSchedule;
    }

    public void setMaintenanceWindowSchedule(MaintenanceWindowSchedule maintenanceWindowSchedule) {
        this.maintenanceWindowSchedule = maintenanceWindowSchedule;
    }

    public Long getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(Long windowStart) {
        this.windowStart = windowStart;
    }

    public Long getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Long windowEnd) {
        this.windowEnd = windowEnd;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowSkip{" +
                "id=" + id +
                ", maintenanceWindowScheduleId=" + (maintenanceWindowSchedule != null ? maintenanceWindowSchedule.getId() : null) +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", timezone='" + timezone + '\'' +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
