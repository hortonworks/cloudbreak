package com.sequenceiq.cloudbreak.service

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED

abstract class ClusterBasedStatusCheckerTask<T : StackContext> : SimpleStatusCheckerTask<T>() {

    override fun exitPolling(t: T): Boolean {
        try {
            val stackId = t.stack.id
            val clusterId = t.stack.cluster.id
            val stackPollGroup = InMemoryStateStore.getStack(stackId)
            if (stackPollGroup == null || stackPollGroup != null && CANCELLED == stackPollGroup) {
                LOGGER.warn("Stack is getting terminated, polling is cancelled.")
                return true
            }

            val clusterPollGroup = InMemoryStateStore.getCluster(clusterId)
            if (clusterPollGroup == null || clusterPollGroup != null && CANCELLED == clusterPollGroup) {
                LOGGER.warn("Cluster is getting terminated, polling is cancelled.")
                return true
            }

            return false
        } catch (ex: Exception) {
            LOGGER.error("Error occurred when check status checker exit criteria: ", ex)
            return true
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterBasedStatusCheckerTask<StackContext>::class.java)
    }
}
