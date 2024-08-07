package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.List;
import java.util.StringJoiner;

import jakarta.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RepairClusterNodeRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class ClusterRepairNodesV4Request {

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

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterRepairNodesV4Request.class.getSimpleName() + "[", "]")
                .add("deleteVolumes=" + deleteVolumes)
                .add("ids=" + ids)
                .toString();
    }
}
