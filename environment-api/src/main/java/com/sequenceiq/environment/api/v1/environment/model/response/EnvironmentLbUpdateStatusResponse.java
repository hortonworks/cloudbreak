package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.LoadBalancerUpdateStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ApiModel(value = "EnvironmentLbUpdateStatusResponse")
public class EnvironmentLbUpdateStatusResponse {

    public static final String NO_ERROR = "None";

    public static final String ENV_ERROR = "The environment update failed. There may have been an error persisting the updates " +
        "to the database, or starting the cluster update processes. Please review the environment logs.";

    public static final String FINISHED_NO_CHILDREN = "Flow finished with no child processes. If any data lakes or data hubs are attached " +
        "to the environment, they have not been updated. Try re-running the process.";

    public static final String NO_CLUSTER_STATUS = "Cluster process status is not yet available. Check again in a few minutes.";

    public static final String CLUSTER_ERROR = "One or more clusters are in a failed state. Please review individual cluster " +
        "event history/logs for more details.";

    public static final String IN_PROGRESS = "Update is running in data lake/data hub clusters.";

    public static final String FINISHED = "Update completed successfully.";

    public static final String MISSING_CHILD_FLOWS = "Status is in an ambiguous state because one or more flows could not be queried. Try running " +
        "the status command again in a few minutes. If this message persists, the update operation may be in a failed state.";

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_CHILD_STATUS)
    private List<ClusterLbUpdateStatus> clusterStatus = new ArrayList<>();

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_STATUS)
    private LoadBalancerUpdateStatus overallStatus = LoadBalancerUpdateStatus.NOT_STARTED;

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_ERROR)
    private String statusReason = NO_ERROR;

    @ApiModelProperty(EnvironmentModelDescription.LB_UPDATE_FLOWID)
    private FlowIdentifier environmentFlowId;

    public List<ClusterLbUpdateStatus> getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(List<ClusterLbUpdateStatus> clusterStatus) {
        this.clusterStatus = clusterStatus;
    }

    public void addChildStatus(String name, String crn, FlowIdentifier flowId, LoadBalancerUpdateStatus updateStatus, String currentState) {
        ClusterLbUpdateStatus status = new ClusterLbUpdateStatus();
        status.setClusterName(name);
        status.setClusterCrn(crn);
        status.setFlowId(flowId);
        status.setStatus(updateStatus);
        status.setCurrentState(currentState);
        clusterStatus.add(status);
    }

    public LoadBalancerUpdateStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(LoadBalancerUpdateStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public FlowIdentifier getEnvironmentFlowId() {
        return environmentFlowId;
    }

    public void setEnvironmentFlowId(FlowIdentifier environmentFlowId) {
        this.environmentFlowId = environmentFlowId;
    }

    public void markEnvironmentUpdateFailed() {
        overallStatus = LoadBalancerUpdateStatus.FAILED;
        statusReason = ENV_ERROR;
    }

    public void markClusterUpdateFailed() {
        overallStatus = LoadBalancerUpdateStatus.FAILED;
        statusReason = CLUSTER_ERROR;
    }

    public void markFinishedNoChildren() {
        overallStatus = LoadBalancerUpdateStatus.FINISHED;
        statusReason = FINISHED_NO_CHILDREN;
    }

    public void markNoClusterStatus() {
        overallStatus = LoadBalancerUpdateStatus.IN_PROGRESS;
        statusReason = NO_CLUSTER_STATUS;
    }

    public void markInProgress() {
        overallStatus = LoadBalancerUpdateStatus.IN_PROGRESS;
        statusReason = IN_PROGRESS;
    }

    public void markFinished() {
        overallStatus = LoadBalancerUpdateStatus.FINISHED;
        statusReason = FINISHED;
    }

    public void markMissingChildFlows() {
        overallStatus = LoadBalancerUpdateStatus.AMBIGUOUS;
        statusReason = MISSING_CHILD_FLOWS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnvironmentLbUpdateStatusResponse that = (EnvironmentLbUpdateStatusResponse) o;
        return Objects.equals(clusterStatus, that.clusterStatus) &&
            overallStatus == that.overallStatus &&
            Objects.equals(statusReason, that.statusReason) &&
            Objects.equals(environmentFlowId, that.environmentFlowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterStatus, overallStatus, statusReason, environmentFlowId);
    }
}
