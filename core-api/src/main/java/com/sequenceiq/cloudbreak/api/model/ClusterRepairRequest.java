package com.sequenceiq.cloudbreak.api.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ClusterRepairRequest {
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RepairClusterRequest.FAILED_NODES, required = true)
    private List<String> failedNodes;

    public List<String> getFailedNodes() {
        return failedNodes;
    }

    public void setFailedNodes(List<String> failedNodes) {
        this.failedNodes = failedNodes;
    }
}
