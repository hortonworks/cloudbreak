package com.sequenceiq.maintenance.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import com.sequenceiq.maintenance.domain.converter.MaintenanceRunStatusConverter;

@Entity
@Table(
        name = "maintenance_window_run",
        uniqueConstraints = @UniqueConstraint(columnNames = {"maintenance_window_task_id", "window_start"})
)
public class MaintenanceWindowRun implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "maintenance_window_run_generator")
    @SequenceGenerator(name = "maintenance_window_run_generator", sequenceName = "maintenance_window_run_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_window_task_id", nullable = false)
    private MaintenanceWindowTask maintenanceWindowTask;

    @Column(name = "resource_crn", nullable = false)
    private String resourceCrn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_schedule_id")
    private MaintenanceWindowSchedule maintenanceWindowSchedule;

    @Column(name = "window_start", nullable = false)
    private Long windowStart;

    @Column(name = "window_end", nullable = false)
    private Long windowEnd;

    @Convert(converter = MaintenanceRunStatusConverter.class)
    @Column(nullable = false)
    private MaintenanceRunStatus status;

    @Column(name = "policy_revision", nullable = false)
    private String policyRevision;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "window_execution_start")
    private Long windowExecutionStart;

    @Column(name = "window_execution_end")
    private Long windowExecutionEnd;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

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

    public MaintenanceWindowTask getMaintenanceWindowTask() {
        return maintenanceWindowTask;
    }

    public void setMaintenanceWindowTask(MaintenanceWindowTask maintenanceWindowTask) {
        this.maintenanceWindowTask = maintenanceWindowTask;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
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

    public MaintenanceRunStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceRunStatus status) {
        this.status = status;
    }

    public String getPolicyRevision() {
        return policyRevision;
    }

    public void setPolicyRevision(String policyRevision) {
        this.policyRevision = policyRevision;
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

    public Long getWindowExecutionStart() {
        return windowExecutionStart;
    }

    public void setWindowExecutionStart(Long windowExecutionStart) {
        this.windowExecutionStart = windowExecutionStart;
    }

    public Long getWindowExecutionEnd() {
        return windowExecutionEnd;
    }

    public void setWindowExecutionEnd(Long windowExecutionEnd) {
        this.windowExecutionEnd = windowExecutionEnd;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "MaintenanceWindowRun{" +
                "id=" + id +
                ", accountId='" + accountId + '\'' +
                ", maintenanceWindowTaskId=" + (maintenanceWindowTask != null ? maintenanceWindowTask.getId() : null) +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", maintenanceWindowScheduleId=" + (maintenanceWindowSchedule != null ? maintenanceWindowSchedule.getId() : null) +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", status=" + status +
                ", policyRevision='" + policyRevision + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", windowExecutionStart=" + windowExecutionStart +
                ", windowExecutionEnd=" + windowExecutionEnd +
                ", errorDetail='" + errorDetail + '\'' +
                ", version=" + version +
                '}';
    }
}
