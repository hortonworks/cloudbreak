package com.sequenceiq.sdx.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxRepairRequest {

    @ApiModelProperty(ModelDescriptions.HOST_GROUP_NAME)
    private String hostGroupName;

    @ApiModelProperty(ModelDescriptions.HOST_GROUP_NAMES)
    private List<String> hostGroupNames;

    @ApiModelProperty(ModelDescriptions.NODE_IDS)
    private List<String> nodesIds;

    @ApiModelProperty(ModelDescriptions.DELETE_VOLUMES)
    private boolean deleteVolumes;

    public String getHostGroupName() {
        return hostGroupName;
    }

    public void setHostGroupName(String hostGroupName) {
        this.hostGroupName = hostGroupName;
    }

    public List<String> getHostGroupNames() {
        return hostGroupNames;
    }

    public void setHostGroupNames(List<String> hostGroupNames) {
        this.hostGroupNames = hostGroupNames;
    }

    public List<String> getNodesIds() {
        return nodesIds;
    }

    public void setNodesIds(List<String> nodesIds) {
        this.nodesIds = nodesIds;
    }

    public boolean isDeleteVolumes() {
        return deleteVolumes;
    }

    public void setDeleteVolumes(boolean deleteVolumes) {
        this.deleteVolumes = deleteVolumes;
    }

    @Override
    public String toString() {
        return "SdxRepairRequest{" +
                "hostGroupName='" + hostGroupName + '\'' +
                ", hostGroupNames=" + hostGroupNames +
                ", nodesIds=" + nodesIds +
                ", deleteVolumes=" + deleteVolumes +
                '}';
    }
}