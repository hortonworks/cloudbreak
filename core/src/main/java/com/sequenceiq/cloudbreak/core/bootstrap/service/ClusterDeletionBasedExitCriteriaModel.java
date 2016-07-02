package com.sequenceiq.cloudbreak.core.bootstrap.service;

import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class ClusterDeletionBasedExitCriteriaModel extends ExitCriteriaModel {
    private Long stackId;
    private Long clusterId;

    public ClusterDeletionBasedExitCriteriaModel(Long stackId, Long clusterId) {
        this.stackId = stackId;
        this.clusterId = clusterId;
    }

    public Long getStackId() {
        return stackId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public static ExitCriteriaModel clusterDeletionBasedExitCriteriaModel(Long stackId, Long clusterId) {
        return new ClusterDeletionBasedExitCriteriaModel(stackId, clusterId);
    }

    @Override
    public String toString() {
        return "ClusterDeletionBasedExitCriteriaModel{"
                + "stackId=" + stackId
                + ", clusterId=" + clusterId + '}';
    }
}
