package com.sequenceiq.cloudbreak.core.bootstrap.service

import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

class ClusterDeletionBasedExitCriteriaModel(val stackId: Long?, val clusterId: Long?) : ExitCriteriaModel() {

    override fun toString(): String {
        return "ClusterDeletionBasedExitCriteriaModel{"
        +"stackId=" + stackId
        +", clusterId=" + clusterId + '}'
    }

    companion object {

        fun clusterDeletionBasedExitCriteriaModel(stackId: Long?, clusterId: Long?): ExitCriteriaModel {
            return ClusterDeletionBasedExitCriteriaModel(stackId, clusterId)
        }
    }
}
