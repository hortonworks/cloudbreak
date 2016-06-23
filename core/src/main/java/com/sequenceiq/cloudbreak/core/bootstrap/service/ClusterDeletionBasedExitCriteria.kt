package com.sequenceiq.cloudbreak.core.bootstrap.service

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED

class ClusterDeletionBasedExitCriteria : ExitCriteria {

    override fun isExitNeeded(exitCriteriaModel: ExitCriteriaModel): Boolean {
        val model = exitCriteriaModel as ClusterDeletionBasedExitCriteriaModel
        LOGGER.debug("Check isExitNeeded for model: {}", model)

        val stackPollGroup = InMemoryStateStore.getStack(model.stackId)
        if (stackPollGroup == null || stackPollGroup != null && CANCELLED == stackPollGroup) {
            LOGGER.warn("Stack is getting terminated, polling is cancelled.")
            return true
        }
        if (model.clusterId != null) {
            val clusterPollGroup = InMemoryStateStore.getCluster(model.clusterId)
            if (clusterPollGroup == null || clusterPollGroup != null && CANCELLED == clusterPollGroup) {
                LOGGER.warn("Cluster is getting terminated, polling is cancelled.")
                return true
            }
        }

        return false
    }

    override fun exitMessage(): String {
        return "Cluster or it's stack is getting terminated, polling is cancelled."
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterDeletionBasedExitCriteria::class.java)
    }
}
