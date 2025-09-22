package com.sequenceiq.common.api.diagnostics;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.telemetry.doc.DiagnosticsModelDescription;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.VmLog;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseDiagnosticsCollectionRequest {

    @Schema(description = DiagnosticsModelDescription.ISSUE)
    private String issue;

    @Schema(description = DiagnosticsModelDescription.DESCRIPTION)
    private String description;

    @Schema(description = DiagnosticsModelDescription.LABELS)
    private List<String> labels;

    @Schema(description = DiagnosticsModelDescription.START_TIME)
    private Date startTime;

    @Schema(description = DiagnosticsModelDescription.END_TIME)
    private Date endTime;

    @NotNull
    @Schema(description = DiagnosticsModelDescription.DESTINATION)
    private DiagnosticsDestination destination;

    @Schema(description = DiagnosticsModelDescription.HOST_GROUPS)
    private Set<String> hostGroups = new HashSet<>();

    @Schema(description = DiagnosticsModelDescription.HOSTS)
    private Set<String> hosts = new HashSet<>();

    @Schema(description = DiagnosticsModelDescription.EXCLUDE_HOSTS)
    private Set<String> excludeHosts = new HashSet<>();

    @Schema(description = DiagnosticsModelDescription.ADDITIONAL_LOGS)
    private List<VmLog> additionalLogs = List.of();

    @Schema(description = DiagnosticsModelDescription.INCLUDE_SALT_LOGS)
    private Boolean includeSaltLogs = Boolean.FALSE;

    @Schema(description = DiagnosticsModelDescription.INCLUDE_SAR_OUTPUT)
    private Boolean includeSarOutput = Boolean.FALSE;

    @Schema(description = DiagnosticsModelDescription.INCLUDE_NGINX_REPORT)
    private Boolean includeNginxReport = Boolean.FALSE;

    @Schema(description = DiagnosticsModelDescription.INCLUDE_SELINUX_REPORT)
    private Boolean includeSeLinuxReport = Boolean.FALSE;

    @Schema(description = DiagnosticsModelDescription.UPDATE_PACKAGE)
    private Boolean updatePackage = Boolean.FALSE;

    @Schema(description = DiagnosticsModelDescription.SKIP_VALIDATION)
    private Boolean skipValidation = Boolean.FALSE;

    @Schema(description = DiagnosticsModelDescription.SKIP_WORKSPACE_CLEANUP)
    private Boolean skipWorkspaceCleanupOnStartup = Boolean.FALSE;

    @Schema(description = DiagnosticsModelDescription.SKIP_UNRESPONSIVE_HOSTS)
    private Boolean skipUnresponsiveHosts = Boolean.FALSE;

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public DiagnosticsDestination getDestination() {
        return destination;
    }

    public void setDestination(DiagnosticsDestination destination) {
        this.destination = destination;
    }

    public List<VmLog> getAdditionalLogs() {
        return additionalLogs;
    }

    public void setAdditionalLogs(List<VmLog> additionalLogs) {
        this.additionalLogs = additionalLogs;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Set<String> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<String> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public void setHosts(Set<String> hosts) {
        this.hosts = hosts;
    }

    public Boolean getIncludeSaltLogs() {
        return includeSaltLogs;
    }

    public void setIncludeSaltLogs(Boolean includeSaltLogs) {
        this.includeSaltLogs = includeSaltLogs;
    }

    public Boolean getIncludeSarOutput() {
        return includeSarOutput;
    }

    public void setIncludeSarOutput(Boolean includeSarOutput) {
        this.includeSarOutput = includeSarOutput;
    }

    public Boolean getIncludeNginxReport() {
        return includeNginxReport;
    }

    public void setIncludeNginxReport(Boolean includeNginxReport) {
        this.includeNginxReport = includeNginxReport;
    }

    public Boolean getIncludeSeLinuxReport() {
        return includeSeLinuxReport;
    }

    public void setIncludeSeLinuxReport(Boolean includeSeLinuxReport) {
        this.includeSeLinuxReport = includeSeLinuxReport;
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

    public Boolean getSkipWorkspaceCleanupOnStartup() {
        return skipWorkspaceCleanupOnStartup;
    }

    public void setSkipWorkspaceCleanupOnStartup(Boolean skipWorkspaceCleanupOnStartup) {
        this.skipWorkspaceCleanupOnStartup = skipWorkspaceCleanupOnStartup;
    }

    public Boolean getSkipUnresponsiveHosts() {
        return skipUnresponsiveHosts;
    }

    public void setSkipUnresponsiveHosts(Boolean skipUnresponsiveHosts) {
        this.skipUnresponsiveHosts = skipUnresponsiveHosts;
    }

    public Set<String> getExcludeHosts() {
        return excludeHosts;
    }

    public void setExcludeHosts(Set<String> excludeHosts) {
        this.excludeHosts = excludeHosts;
    }
}
