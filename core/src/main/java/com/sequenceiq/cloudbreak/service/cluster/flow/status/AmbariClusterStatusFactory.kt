package com.sequenceiq.cloudbreak.service.cluster.flow.status

import java.util.EnumSet
import java.util.HashSet
import java.util.TreeSet

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.ambari.client.AmbariClient

@Component
class AmbariClusterStatusFactory {

    private val partialStatuses = EnumSet.of(ClusterStatus.INSTALLING, ClusterStatus.INSTALL_FAILED, ClusterStatus.STARTING,
            ClusterStatus.STOPPING)
    private val fullStatuses = EnumSet.of(ClusterStatus.INSTALLED, ClusterStatus.STARTED)

    fun createClusterStatus(ambariClient: AmbariClient, blueprint: String?): ClusterStatus {
        val clusterStatus: ClusterStatus
        if (!isAmbariServerRunning(ambariClient)) {
            clusterStatus = ClusterStatus.AMBARISERVER_NOT_RUNNING
        } else if (blueprint != null) {
            clusterStatus = determineClusterStatus(ambariClient, blueprint)
        } else {
            clusterStatus = ClusterStatus.AMBARISERVER_RUNNING
        }
        return clusterStatus
    }

    private fun isAmbariServerRunning(ambariClient: AmbariClient): Boolean {
        val result: Boolean
        try {
            result = "RUNNING" == ambariClient.healthCheck()
        } catch (ex: Exception) {
            result = false
        }

        return result
    }

    private fun determineClusterStatus(ambariClient: AmbariClient, blueprint: String): ClusterStatus {
        val clusterStatus: ClusterStatus
        try {
            val ambariOperations = ambariClient.getRequests("IN_PROGRESS", "PENDING")
            if (!ambariOperations.isEmpty()) {
                clusterStatus = ClusterStatus.PENDING
            } else {
                val orderedPartialStatuses = TreeSet<ClusterStatus>()
                val orderedFullStatuses = TreeSet<ClusterStatus>()
                val unsupportedStatuses = HashSet<String>()
                val componentsCategory = ambariClient.getComponentsCategory(blueprint)
                val hostComponentsStates = ambariClient.hostComponentsStates
                for (hostComponentsEntry in hostComponentsStates.entries) {
                    val componentStateMap = hostComponentsEntry.value
                    for (componentStateEntry in componentStateMap.entries) {
                        val category = componentsCategory[componentStateEntry.key]
                        if ("CLIENT" != category) {
                            putComponentState(componentStateEntry.value, orderedPartialStatuses, orderedFullStatuses, unsupportedStatuses)
                        }
                    }
                }
                clusterStatus = determineClusterStatus(orderedPartialStatuses, orderedFullStatuses)
            }
        } catch (ex: Exception) {
            LOGGER.warn("An error occurred while trying to reach Ambari.", ex)
            clusterStatus = ClusterStatus.UNKNOWN
        }

        return clusterStatus
    }

    private fun determineClusterStatus(orderedPartialStatuses: Set<ClusterStatus>, orderedFullStatuses: Set<ClusterStatus>): ClusterStatus {
        val clusterStatus: ClusterStatus
        if (!orderedPartialStatuses.isEmpty()) {
            clusterStatus = orderedPartialStatuses.iterator().next()
        } else if (orderedFullStatuses.size == 1) {
            clusterStatus = orderedFullStatuses.iterator().next()
        } else {
            clusterStatus = ClusterStatus.AMBIGUOUS
        }
        return clusterStatus
    }

    private fun putComponentState(componentStateStr: String, orderedPartialStatuses: MutableSet<ClusterStatus>, orderedFullStatuses: MutableSet<ClusterStatus>,
                                  unsupportedStatuses: MutableSet<String>) {
        try {
            val componentStatus = ClusterStatus.valueOf(componentStateStr)
            if (partialStatuses.contains(componentStatus)) {
                orderedPartialStatuses.add(componentStatus)
            } else if (fullStatuses.contains(componentStatus)) {
                orderedFullStatuses.add(componentStatus)
            } else {
                unsupportedStatuses.add(componentStateStr)
            }
        } catch (ex: RuntimeException) {
            unsupportedStatuses.add(componentStateStr)
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AmbariClusterStatusFactory::class.java)
    }
}
