package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.LoadBalancerUpdateStatus;

@ApiModel(value = "ClusterLbUpdateStatus")
public class ClusterLbUpdateStatus {

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_CHILD_NAME)
    private String clusterName;

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_CHILD_CRN)
    private String clusterCrn;

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_CHILD_FLOWID)
    private String flowId;

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_CHILD_STATUS)
    private LoadBalancerUpdateStatus status;

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_CHILD_STATE)
    private String currentState;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public void setClusterCrn(String clusterCrn) {
        this.clusterCrn = clusterCrn;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public LoadBalancerUpdateStatus getStatus() {
        return status;
    }

    public void setStatus(LoadBalancerUpdateStatus status) {
        this.status = status;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClusterLbUpdateStatus that = (ClusterLbUpdateStatus) o;
        return Objects.equals(clusterName, that.clusterName) &&
            Objects.equals(clusterCrn, that.clusterCrn) &&
            Objects.equals(flowId, that.flowId) &&
            status == that.status &&
            Objects.equals(currentState, that.currentState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterName, clusterCrn, flowId, status, currentState);
    }
}
