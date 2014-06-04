package com.sequenceiq.cloudbreak.controller.json;

public class ClusterRequest {

    private String clusterName;
    private Long blueprintId;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

}
