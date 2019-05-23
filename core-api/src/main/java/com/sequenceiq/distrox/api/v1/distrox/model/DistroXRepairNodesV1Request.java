package com.sequenceiq.distrox.api.v1.distrox.model;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RepairClusterNodeRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DistroXRepairNodesV1Request {

    @ApiModelProperty(RepairClusterNodeRequest.DELETE_VOLUMES)
    private boolean deleteVolumes;

    @ApiModelProperty(RepairClusterNodeRequest.IDS)
    @NotEmpty(message = "Node ID list must not be empty")
    private List<String> ids;

    public boolean isDeleteVolumes() {
        return deleteVolumes;
    }

    public void setDeleteVolumes(boolean deleteVolumes) {
        this.deleteVolumes = deleteVolumes;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
