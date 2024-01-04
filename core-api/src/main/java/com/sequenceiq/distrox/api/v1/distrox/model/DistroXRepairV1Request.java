package com.sequenceiq.distrox.api.v1.distrox.model;

import java.util.List;

import jakarta.validation.Valid;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RepairClusterRequest;
import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@MutuallyExclusiveNotNull(fieldGroups = {"hostGroups", "nodes"}, message = "Either hostGroups or nodes should be provided but not both.")
public class DistroXRepairV1Request {

    @Schema(description = RepairClusterRequest.HOSTGROUPS, required = true)
    private List<String> hostGroups;

    @Schema(description = RepairClusterRequest.NODES)
    @Valid
    private DistroXRepairNodesV1Request nodes;

    @Deprecated
    @Schema(description = RepairClusterRequest.REMOVE_ONLY)
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

    public DistroXRepairNodesV1Request getNodes() {
        return nodes;
    }

    public void setNodes(DistroXRepairNodesV1Request nodes) {
        this.nodes = nodes;
    }
}
