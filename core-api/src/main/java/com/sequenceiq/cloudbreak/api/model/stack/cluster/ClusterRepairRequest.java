package com.sequenceiq.cloudbreak.api.model.stack.cluster;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.annotations.MutuallyExclusiveNotNull;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RepairClusterRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@MutuallyExclusiveNotNull(fieldNames = {"hostGroups", "nodes"})
public class ClusterRepairRequest {

    @ApiModelProperty(value = RepairClusterRequest.HOSTGROUPS, required = true)
    private List<String> hostGroups;

    @ApiModelProperty(value = RepairClusterRequest.NODES)
    private List<ClusterRepairNodeRequest> nodes;

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

    public List<ClusterRepairNodeRequest> getNodes() {
        return nodes;
    }

    public void setNodes(List<ClusterRepairNodeRequest> nodes) {
        this.nodes = nodes;
    }
}
