package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ChangedNodesReportV4Request {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NodeStatusChangeReport.NEW_FAILED_NODES, required = true)
    private List<String> newFailedNodes;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NodeStatusChangeReport.NEW_FAILED_NODES, required = true)
    private List<String> newHealthyNodes;

    public List<String> getNewFailedNodes() {
        return newFailedNodes;
    }

    public void setNewFailedNodes(List<String> newFailedNodes) {
        this.newFailedNodes = newFailedNodes;
    }

    public List<String> getNewHealthyNodes() {
        return newHealthyNodes;
    }

    public void setNewHealthyNodes(List<String> newHealthyNodes) {
        this.newHealthyNodes = newHealthyNodes;
    }
}
