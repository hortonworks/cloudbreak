package com.sequenceiq.cloudbreak.core.flow2.cluster.stop

import javax.inject.Inject

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
class ClusterStopService {
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val emailSenderService: EmailSenderService? = null

    fun stoppingCluster(stack: Stack) {
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_STOPPING, Status.UPDATE_IN_PROGRESS.name)
        clusterService!!.updateClusterStatusByStackId(stack.id, Status.STOP_IN_PROGRESS)
    }

    fun clusterStopFinished(stack: Stack, statusBeforeAmbariStop: Status) {
        if (statusBeforeAmbariStop != stack.status) {
            stackUpdater!!.updateStackStatus(stack.id, if (stack.isStopRequested) Status.STOP_REQUESTED else statusBeforeAmbariStop)
        }
        clusterService!!.updateClusterStatusByStackId(stack.id, Status.STOPPED)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_STOPPED, Status.STOPPED.name)
    }

    fun handleClusterStopFailure(stack: Stack, errorReason: String) {
        val cluster = stack.cluster
        clusterService!!.updateClusterStatusByStackId(stack.id, Status.STOP_FAILED)
        stackUpdater!!.updateStackStatus(stack.id, Status.AVAILABLE, "The Ambari cluster could not be stopped: " + errorReason)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_STOP_FAILED, Status.AVAILABLE.name, errorReason)
        if (cluster.emailNeeded!!) {
            emailSenderService!!.sendStopFailureEmail(stack.cluster.owner, stack.ambariIp, cluster.name)
            flowMessageService.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.STOP_FAILED.name)
        }
    }
}
