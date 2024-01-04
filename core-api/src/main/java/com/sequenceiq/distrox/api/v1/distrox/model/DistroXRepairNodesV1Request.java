package com.sequenceiq.distrox.api.v1.distrox.model;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RepairClusterNodeRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class DistroXRepairNodesV1Request {

    @Schema(description = RepairClusterNodeRequest.DELETE_VOLUMES)
    private boolean deleteVolumes;

    @Schema(description = RepairClusterNodeRequest.IDS)
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
