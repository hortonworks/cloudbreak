package com.sequenceiq.common.api.diagnostics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.telemetry.doc.DiagnosticsModelDescription;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseCmDiagnosticsCollectionRequest implements Serializable {

    @NotNull
    @Schema(description = DiagnosticsModelDescription.DESTINATION)
    private DiagnosticsDestination destination;

    @Schema(description = DiagnosticsModelDescription.ROLES)
    private List<String> roles = new ArrayList<>();

    @Schema(description = DiagnosticsModelDescription.BUNDLE_SIZE_BYTES)
    private BigDecimal bundleSizeBytes;

    @Schema(description = DiagnosticsModelDescription.START_TIME)
    private Date startTime;

    @Schema(description = DiagnosticsModelDescription.END_TIME)
    private Date endTime;

    @Schema(description = DiagnosticsModelDescription.TICKET)
    private String ticket;

    @Schema(description = DiagnosticsModelDescription.COMMENTS)
    private String comments;

    @Schema(description = DiagnosticsModelDescription.ENABLE_MONITOR_METRICS_COLLECTION)
    private Boolean enableMonitorMetricsCollection;

    @Schema(description = DiagnosticsModelDescription.UPDATE_PACKAGE)
    private Boolean updatePackage = Boolean.FALSE;

    @Schema(description = DiagnosticsModelDescription.SKIP_VALIDATION)
    private Boolean skipValidation = Boolean.FALSE;

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

    public Boolean getSkipValidation() {
        return skipValidation;
    }

    public void setSkipValidation(Boolean skipValidation) {
        this.skipValidation = skipValidation;
    }
}
