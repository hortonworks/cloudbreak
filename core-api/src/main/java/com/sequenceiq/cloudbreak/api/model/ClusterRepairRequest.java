package com.sequenceiq.cloudbreak.api.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ClusterRepairRequest {
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RepairClusterRequest.HOSTGROUPS, required = true)
    private List<String> hostGroups;

    @ApiModelProperty(value = ModelDescriptions.RepairClusterRequest.REMOVE_ONLY)
    private boolean removeOnly;

    public List<String> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(List<String> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public boolean isRemoveOnly() {
        return removeOnly;
    }

    public void setRemoveOnly(boolean removeOnly) {
        this.removeOnly = removeOnly;
    }
}
