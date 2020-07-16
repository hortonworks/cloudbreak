package com.sequenceiq.common.api.diagnostics;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.telemetry.doc.DiagnosticsModelDescription;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.VmLog;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseDiagnosticsCollectionRequest {

    @ApiModelProperty(DiagnosticsModelDescription.ISSUE)
    private String issue;

    @ApiModelProperty(DiagnosticsModelDescription.DESCRIPTION)
    private String description;

    @ApiModelProperty(DiagnosticsModelDescription.LABELS)
    private List<String> labels;

    @ApiModelProperty(DiagnosticsModelDescription.START_TIME)
    private Date startTime;

    @ApiModelProperty(DiagnosticsModelDescription.END_TIME)
    private Date endTime;

    @NotNull
    @ApiModelProperty(DiagnosticsModelDescription.DESTINATION)
    private DiagnosticsDestination destination;

    private List<VmLog> additionalLogs = List.of();

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
}
