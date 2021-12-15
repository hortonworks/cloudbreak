package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.Optional;

import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class ClusterDeletionBasedExitCriteriaModel extends ExitCriteriaModel {

    private final Long stackId;

    private final Long clusterId;

    public ClusterDeletionBasedExitCriteriaModel(Long stackId, Long clusterId) {
        this.stackId = stackId;
        this.clusterId = clusterId;
    }

    public Optional<Long> getStackId() {
        return Optional.ofNullable(stackId);
    }

    public Optional<Long> getClusterId() {
        return Optional.ofNullable(clusterId);
    }

    public static ClusterDeletionBasedExitCriteriaModel clusterDeletionBasedModel(Long stackId, Long clusterId) {
        return new ClusterDeletionBasedExitCriteriaModel(stackId, clusterId);
    }

    public static ClusterDeletionBasedExitCriteriaModel nonCancellableModel() {
        return new ClusterDeletionBasedExitCriteriaModel(null, null);
    }

    @Override
    public String toString() {
        return "ClusterDeletionBasedExitCriteriaModel{"
                + "stackId=" + stackId
                + ", clusterId=" + clusterId + '}';
    }
}
