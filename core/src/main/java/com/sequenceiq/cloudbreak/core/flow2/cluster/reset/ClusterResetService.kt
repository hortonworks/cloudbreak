package com.sequenceiq.cloudbreak.core.flow2.cluster.reset

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService

@Service
class ClusterResetService {
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val emailSenderService: EmailSenderService? = null

    fun resetCluster(stack: Stack, cluster: Cluster) {
        MDCBuilder.buildMdcContext(cluster)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_RESET, Status.UPDATE_IN_PROGRESS.name)
    }

    fun handleResetClusterFailure(stack: Stack, errorReason: String) {
        val cluster = clusterService!!.retrieveClusterByStackId(stack.id)
        MDCBuilder.buildMdcContext(cluster)
        clusterService.updateClusterStatusByStackId(stack.id, Status.CREATE_FAILED, errorReason)
        stackUpdater!!.updateStackStatus(stack.id, Status.AVAILABLE)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_CREATE_FAILED, Status.CREATE_FAILED.name, errorReason)
        if (cluster.emailNeeded!!) {
            emailSenderService!!.sendProvisioningFailureEmail(cluster.owner, cluster.name)
            flowMessageService.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.AVAILABLE.name)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterResetService::class.java)
    }
}
