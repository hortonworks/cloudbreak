package com.sequenceiq.cloudbreak.api.model.stack.cluster;

import java.util.List;

import javax.validation.Valid;

import com.sequenceiq.cloudbreak.api.model.annotations.MutuallyExclusiveNotNull;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RepairClusterRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@MutuallyExclusiveNotNull(fieldNames = {"hostGroups", "nodes"}, message = "Either hostGroups or nodes should be provided but not both.")
public class ClusterRepairRequest {

    @ApiModelProperty(value = RepairClusterRequest.HOSTGROUPS, required = true)
    private List<String> hostGroups;

    @ApiModelProperty(value = RepairClusterRequest.NODES)
    @Valid
    private ClusterRepairNodesRequest nodes;

    @ApiModelProperty(RepairClusterRequest.REMOVE_ONLY)
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

    public ClusterRepairNodesRequest getNodes() {
        return nodes;
    }

    public void setNodes(ClusterRepairNodesRequest nodes) {
        this.nodes = nodes;
    }
}
