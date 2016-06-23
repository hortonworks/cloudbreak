package com.sequenceiq.cloudbreak.core.flow2.cluster.termination

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED
import com.sequenceiq.cloudbreak.api.model.Status.DELETE_FAILED

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService

@Service
class ClusterTerminationFlowService {
    @Inject
    private val terminationService: ClusterTerminationService? = null
    @Inject
    private val emailSenderService: EmailSenderService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null

    fun terminateCluster(context: ClusterContext) {
        clusterService!!.updateClusterStatusByStackId(context.stack.id, Status.DELETE_IN_PROGRESS)
        LOGGER.info("Cluster delete started.")
    }

    fun finishClusterTermination(context: ClusterContext, payload: ClusterTerminationResult) {
        LOGGER.info("Terminate cluster result: {}", payload)
        val cluster = context.cluster
        terminationService!!.finalizeClusterTermination(cluster.id)
        flowMessageService!!.fireEventAndLog(cluster.stack.id, Msg.CLUSTER_DELETE_COMPLETED, DELETE_COMPLETED.name, cluster.id)
        clusterService!!.updateClusterStatusByStackId(cluster.stack.id, DELETE_COMPLETED)
        InMemoryStateStore.deleteCluster(cluster.id)
        stackUpdater!!.updateStackStatus(cluster.stack.id, AVAILABLE)
        if (cluster.emailNeeded!!) {
            emailSenderService!!.sendTerminationSuccessEmail(cluster.owner, cluster.ambariIp, cluster.name)
            flowMessageService.fireEventAndLog(cluster.stack.id, Msg.CLUSTER_EMAIL_SENT, DELETE_COMPLETED.name)
        }
    }

    fun handleClusterTerminationError(payload: StackFailureEvent) {
        LOGGER.info("Handling cluster delete failure event.")
        val errorDetails = payload.exception
        LOGGER.error("Error during cluster termination flow: ", errorDetails)
        val cluster = clusterService!!.retrieveClusterByStackId(payload.stackId)
        cluster.status = DELETE_FAILED
        cluster.statusReason = errorDetails.message
        clusterService.updateCluster(cluster)
        flowMessageService!!.fireEventAndLog(cluster.stack.id, Msg.CLUSTER_DELETE_FAILED, DELETE_FAILED.name, errorDetails.message)
        if (cluster.emailNeeded!!) {
            emailSenderService!!.sendTerminationFailureEmail(cluster.owner, cluster.ambariIp, cluster.name)
            flowMessageService.fireEventAndLog(cluster.stack.id, Msg.CLUSTER_EMAIL_SENT, DELETE_FAILED.name)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterTerminationFlowService::class.java)
    }
}
