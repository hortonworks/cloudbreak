package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FailureReport;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class FailureReportV4Request {
    @NotNull
    @ApiModelProperty(value = FailureReport.FAILED_NODES, required = true)
    private List<String> failedNodes;

    public List<String> getFailedNodes() {
        return failedNodes;
    }

    public void setFailedNodes(List<String> failedNodes) {
        this.failedNodes = failedNodes;
    }
}
