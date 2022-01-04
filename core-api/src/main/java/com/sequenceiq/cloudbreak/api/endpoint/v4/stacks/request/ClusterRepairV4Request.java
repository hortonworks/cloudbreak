package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.List;
import java.util.StringJoiner;

import javax.validation.Valid;

import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RepairClusterRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@MutuallyExclusiveNotNull(fieldGroups = {"hostGroups", "nodes"}, message = "Either hostGroups or nodes should be provided but not both.")
public class ClusterRepairV4Request {

    @ApiModelProperty(value = RepairClusterRequest.HOSTGROUPS, required = true)
    private List<String> hostGroups;

    @ApiModelProperty(RepairClusterRequest.NODES)
    @Valid
    private ClusterRepairNodesV4Request nodes;

    @Deprecated
    @ApiModelProperty(RepairClusterRequest.REMOVE_ONLY)
    private boolean removeOnly;

    @ApiModelProperty(RepairClusterRequest.RESTART_SERVICES)
    private boolean restartServices;

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

    public ClusterRepairNodesV4Request getNodes() {
        return nodes;
    }

    public void setNodes(ClusterRepairNodesV4Request nodes) {
        this.nodes = nodes;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

    public void setRestartServices(boolean restartServices) {
        this.restartServices = restartServices;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterRepairV4Request.class.getSimpleName() + "[", "]")
                .add("hostGroups=" + hostGroups)
                .add("nodes=" + nodes)
                .toString();
    }
}
