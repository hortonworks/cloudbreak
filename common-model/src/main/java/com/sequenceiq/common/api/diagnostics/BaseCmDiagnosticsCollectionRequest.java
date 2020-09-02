package com.sequenceiq.common.api.diagnostics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;

public abstract class BaseCmDiagnosticsCollectionRequest implements Serializable {

    @NotNull
    private DiagnosticsDestination destination;

    private List<String> roles = new ArrayList<>();

    private BigDecimal bundleSizeBytes;

    private Date startTime;

    private Date endTime;

    private String ticket;

    private String comments;

    private Boolean includeInfoLog;

    private Boolean enableMonitorMetricsCollection;

    private Boolean updatePackage = Boolean.FALSE;

    public abstract String getStackCrn();

    public abstract void setStackCrn(String stackCrn);

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public BigDecimal getBundleSizeBytes() {
        return bundleSizeBytes;
    }

    public void setBundleSizeBytes(BigDecimal bundleSizeBytes) {
        this.bundleSizeBytes = bundleSizeBytes;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Boolean getIncludeInfoLog() {
        return includeInfoLog;
    }

    public void setIncludeInfoLog(Boolean includeInfoLog) {
        this.includeInfoLog = includeInfoLog;
    }

    public Boolean getEnableMonitorMetricsCollection() {
        return enableMonitorMetricsCollection;
    }

    public void setEnableMonitorMetricsCollection(Boolean enableMonitorMetricsCollection) {
        this.enableMonitorMetricsCollection = enableMonitorMetricsCollection;
    }

    public DiagnosticsDestination getDestination() {
        return destination;
    }

    public void setDestination(DiagnosticsDestination destination) {
        this.destination = destination;
    }

    public Boolean getUpdatePackage() {
        return updatePackage;
    }

    public void setUpdatePackage(Boolean updatePackage) {
        this.updatePackage = updatePackage;
    }
}
