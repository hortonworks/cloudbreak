package com.sequenceiq.cloudbreak.core.flow2.cluster.start

import java.util.Date

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService

@Service
class ClusterStartService {
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val emailSenderService: EmailSenderService? = null

    fun startingCluster(stack: Stack, cluster: Cluster) {
        clusterService!!.updateClusterStatusByStackId(stack.id, Status.START_IN_PROGRESS)
        stackUpdater!!.updateStackStatus(stack.id, Status.UPDATE_IN_PROGRESS, String.format("Starting the Ambari cluster. Ambari ip:%s",
                stack.ambariIp))
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_STARTING, Status.UPDATE_IN_PROGRESS.name, stack.ambariIp)
    }

    fun clusterStartFinished(stack: Stack) {
        val cluster = clusterService!!.retrieveClusterByStackId(stack.id)
        cluster.upSince = Date().time
        clusterService.updateCluster(cluster)
        clusterService.updateClusterStatusByStackId(stack.id, Status.AVAILABLE)
        stackUpdater!!.updateStackStatus(stack.id, Status.AVAILABLE, "Ambari cluster started.")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_STARTED, Status.AVAILABLE.name, stack.ambariIp)
        if (cluster.emailNeeded!!) {
            emailSenderService!!.sendStartSuccessEmail(cluster.owner, stack.ambariIp, cluster.name)
            flowMessageService.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.AVAILABLE.name)
        }
    }

    fun handleClusterStartFailure(stack: Stack, errorReason: String) {
        val cluster = stack.cluster
        clusterService!!.updateClusterStatusByStackId(stack.id, Status.START_FAILED)
        stackUpdater!!.updateStackStatus(stack.id, Status.AVAILABLE, "Cluster could not be started: " + errorReason)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_START_FAILED, Status.AVAILABLE.name, errorReason)
        if (cluster.emailNeeded!!) {
            emailSenderService!!.sendStartFailureEmail(stack.cluster.owner, stack.ambariIp, cluster.name)
            flowMessageService.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.START_FAILED.name)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterStartService::class.java)
    }
}
