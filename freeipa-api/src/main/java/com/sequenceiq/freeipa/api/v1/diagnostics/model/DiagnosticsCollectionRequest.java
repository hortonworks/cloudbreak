package com.sequenceiq.freeipa.api.v1.diagnostics.model;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.common.api.telemetry.model.VmLog;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.service.api.doc.ModelDescriptions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("DiagnosticsCollectionV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagnosticsCollectionRequest {

    @NotNull
    @ApiModelProperty(ModelDescriptions.ENVIRONMENT_CRN)
    @ResourceObjectField(action = AuthorizationResourceAction.ENVIRONMENT_READ, variableType = AuthorizationVariableType.CRN)
    private String environmentCrn;

    @ApiModelProperty(ModelDescriptions.ISSUE)
    private String issue;

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(ModelDescriptions.LABELS)
    private List<String> labels;

    @ApiModelProperty(ModelDescriptions.START_TIME)
    private Date startTime;

    @ApiModelProperty(ModelDescriptions.END_TIME)
    private Date endTime;

    @NotNull
    @ApiModelProperty(ModelDescriptions.DESTINATION)
    private DiagnosticsDestination destination;

    private List<VmLog> additionalLogs = List.of();

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

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
