package com.sequenceiq.cloudbreak.core.flow2.cluster.provision

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.CREATE_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException

@Component
class ClusterCreationService {

    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val orchestratorTypeResolver: OrchestratorTypeResolver? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val emailSenderService: EmailSenderService? = null
    @Inject
    private val clusterTerminationService: ClusterTerminationService? = null

    @Throws(CloudbreakException::class)
    fun startingAmbariServices(stack: Stack, cluster: Cluster) {
        val orchestrator = stack.orchestrator
        val orchestratorType = orchestratorTypeResolver!!.resolveType(orchestrator.type)
        stackUpdater!!.updateStackStatus(stack.id, UPDATE_IN_PROGRESS, "Running cluster services.")
        if (orchestratorType.containerOrchestrator()) {
            flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_RUN_CONTAINERS, UPDATE_IN_PROGRESS.name)
        } else if (orchestratorType.hostOrchestrator()) {
            flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_RUN_SERVICES, UPDATE_IN_PROGRESS.name)
        } else {
            LOGGER.info(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.type))
            throw CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.type))
        }
    }

    fun startingAmbari(stack: Stack) {
        stackUpdater!!.updateStackStatus(stack.id, UPDATE_IN_PROGRESS, "Ambari cluster is now starting.")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_STARTING, UPDATE_IN_PROGRESS.name)
        clusterService!!.updateClusterStatusByStackId(stack.id, UPDATE_IN_PROGRESS)
    }

    fun installingCluster(stack: Stack) {
        stackUpdater!!.updateStackStatus(stack.id, UPDATE_IN_PROGRESS, String.format("Building the Ambari cluster. Ambari ip:%s", stack.ambariIp))
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_BUILDING, UPDATE_IN_PROGRESS.name, stack.ambariIp)
    }

    fun clusterInstallationFinished(stack: Stack, cluster: Cluster) {
        clusterService!!.updateClusterStatusByStackId(stack.id, AVAILABLE)
        stackUpdater!!.updateStackStatus(stack.id, AVAILABLE, "Cluster creation finished.")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_BUILT, AVAILABLE.name, stack.ambariIp)
        if (cluster.emailNeeded!!) {
            emailSenderService!!.sendProvisioningSuccessEmail(cluster.owner, stack.ambariIp, cluster.name)
            flowMessageService.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name)
        }
    }

    fun handleClusterCreationFailure(stack: Stack, exception: Exception) {
        val cluster = stack.cluster
        clusterService!!.updateClusterStatusByStackId(stack.id, CREATE_FAILED, exception.message)
        stackUpdater!!.updateStackStatus(stack.id, AVAILABLE)
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_CREATE_FAILED, CREATE_FAILED.name, exception.message)

        // TODO Only triggering the deleteClusterContainers flow
        try {
            val orchestratorType = orchestratorTypeResolver!!.resolveType(stack.orchestrator.type)
            if (cluster != null && orchestratorType.containerOrchestrator()) {
                clusterTerminationService!!.deleteClusterContainers(cluster)
            }
        } catch (ex: CloudbreakException) {
            LOGGER.error("Cluster containers could not be deleted, preparation for reinstall failed: ", ex)
        } catch (ex: TerminationFailedException) {
            LOGGER.error("Cluster containers could not be deleted, preparation for reinstall failed: ", ex)
        }

        if (cluster!!.emailNeeded!!) {
            emailSenderService!!.sendProvisioningFailureEmail(cluster.owner, cluster.name)
            flowMessageService.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterCreationService::class.java)
    }
}
