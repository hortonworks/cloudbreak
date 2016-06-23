package com.sequenceiq.cloudbreak.service.cluster.flow.status

import java.util.Arrays

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

@Component
class AmbariClusterStatusUpdater {

    @Inject
    private val clusterService: ClusterService? = null

    @Inject
    private val ambariClientProvider: AmbariClientProvider? = null

    @Inject
    private val cloudbreakEventService: CloudbreakEventService? = null

    @Inject
    private val clusterStatusFactory: AmbariClusterStatusFactory? = null

    @Inject
    private val tlsSecurityService: TlsSecurityService? = null

    @Inject
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null

    private enum class Msg private constructor(private val code: String) {
        AMBARI_CLUSTER_COULD_NOT_SYNC("ambari.cluster.could.not.sync"),
        AMBARI_CLUSTER_SYNCHRONIZED("ambari.cluster.synchronized");

        fun code(): String {
            return code
        }
    }


    @Throws(CloudbreakSecuritySetupException::class)
    fun updateClusterStatus(stack: Stack, cluster: Cluster?) {
        if (isStackOrClusterStatusInvalid(stack, cluster)) {
            val msg = cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_COULD_NOT_SYNC.code(), Arrays.asList<Comparable<out Comparable<*>>>(stack.status,
                    if (cluster == null) "" else cluster.status))
            LOGGER.warn(msg)
            cloudbreakEventService!!.fireCloudbreakEvent(stack.id, stack.status.name, msg)
        } else {
            val stackId = stack.id
            val blueprintName = if (cluster != null) cluster.blueprint.blueprintName else null
            val clientConfig = tlsSecurityService!!.buildTLSClientConfig(stackId, cluster!!.ambariIp)
            if (cluster.ambariIp != null) {
                clusterService!!.updateClusterMetadata(stackId)
                val clusterStatus = clusterStatusFactory!!.createClusterStatus(ambariClientProvider!!.getAmbariClient(
                        clientConfig, stack.gatewayPort, cluster.userName, cluster.password), blueprintName)
                updateClusterStatus(stackId, stack.status, cluster, clusterStatus)
            }
        }
    }

    private fun isStackOrClusterStatusInvalid(stack: Stack, cluster: Cluster?): Boolean {
        return stack.isStackInDeletionPhase
                || stack.isStackInStopPhase
                || stack.isModificationInProgress
                || cluster == null
                || cluster.isModificationInProgress
    }

    private fun updateClusterStatus(stackId: Long?, stackStatus: Status, cluster: Cluster, ambariClusterStatus: ClusterStatus) {
        var statusInEvent = stackStatus
        var statusReason = ambariClusterStatus.statusReason
        if (isUpdateEnabled(ambariClusterStatus)) {
            if (updateClusterStatus(stackId, cluster, ambariClusterStatus.clusterStatus)) {
                statusInEvent = ambariClusterStatus.stackStatus
                statusReason = ambariClusterStatus.statusReason
            } else {
                statusReason = "The cluster's state is up to date."
            }
        }
        cloudbreakEventService!!.fireCloudbreakEvent(stackId, statusInEvent.name, cloudbreakMessagesService!!.getMessage(Msg.AMBARI_CLUSTER_SYNCHRONIZED.code(),
                Arrays.asList(statusReason)))
    }

    private fun isUpdateEnabled(clusterStatus: ClusterStatus): Boolean {
        return clusterStatus == ClusterStatus.STARTED || clusterStatus == ClusterStatus.INSTALLED
    }

    private fun updateClusterStatus(stackId: Long?, cluster: Cluster, newClusterStatus: Status): Boolean {
        var result = false
        if (cluster.status != newClusterStatus) {
            LOGGER.info("Cluster {} status is updated from {} to {}", cluster.id, cluster.status, newClusterStatus)
            clusterService!!.updateClusterStatusByStackId(stackId, newClusterStatus)
            result = true
        } else {
            LOGGER.info("Cluster {} status hasn't changed: {}", cluster.id, cluster.status)
        }
        return result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AmbariClusterStatusUpdater::class.java)
    }
}
