package com.sequenceiq.cloudbreak.api.model.stack.cluster;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RepairClusterNodeRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ClusterRepairNodesRequest {

    @ApiModelProperty(RepairClusterNodeRequest.DELETE_VOLUMES)
    private boolean deleteVolumes;

    @NotEmpty
    @ApiModelProperty(RepairClusterNodeRequest.IDS)
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
