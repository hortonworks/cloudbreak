package com.sequenceiq.cloudbreak.core.init

import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_REQUESTED
import com.sequenceiq.cloudbreak.api.model.Status.WAIT_FOR_SYNC

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.stream.Collectors

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.FlowLogRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService

@Component
class CloudbreakCleanupAction {

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val clusterRepository: ClusterRepository? = null

    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Inject
    private val eventService: CloudbreakEventService? = null

    @Inject
    private val flowLogRepository: FlowLogRepository? = null

    @Inject
    private val flowLogService: FlowLogService? = null

    @Inject
    private val flowManager: ReactorFlowManager? = null

    fun resetStates() {
        val stacksToSync = resetStackStatus()
        val clustersToSync = resetClusterStatus(stacksToSync)
        setDeleteFailedStatus()
        terminateRunningFlows()
        triggerSyncs(stacksToSync, clustersToSync)
    }

    private fun resetStackStatus(): List<Stack> {
        val stacksInProgress = stackRepository!!.findByStatuses(Arrays.asList(UPDATE_REQUESTED, UPDATE_IN_PROGRESS, WAIT_FOR_SYNC))
        for (stack in stacksInProgress) {
            if (WAIT_FOR_SYNC != stack.status) {
                loggingStatusChange("Stack", stack.id, stack.status, WAIT_FOR_SYNC)
                stack.status = WAIT_FOR_SYNC
                stackRepository.save(stack)
            }
            cleanInstanceMetaData(instanceMetaDataRepository!!.findAllInStack(stack.id))
        }
        return stacksInProgress
    }

    private fun cleanInstanceMetaData(metadataSet: Set<InstanceMetaData>) {
        for (metadata in metadataSet) {
            if (InstanceStatus.REQUESTED == metadata.instanceStatus && metadata.instanceId == null) {
                LOGGER.info("InstanceMetaData [privateId: '{}'] is deleted at CB start.", metadata.privateId)
                instanceMetaDataRepository!!.delete(metadata)
            }
        }
    }

    private fun resetClusterStatus(stacksToSync: List<Stack>): List<Cluster> {
        val clustersInProgress = clusterRepository!!.findByStatuses(Arrays.asList(UPDATE_REQUESTED, UPDATE_IN_PROGRESS, WAIT_FOR_SYNC))
        val clustersToSync = ArrayList<Cluster>()
        for (cluster in clustersInProgress) {
            loggingStatusChange("Cluster", cluster.id, cluster.status, WAIT_FOR_SYNC)
            cluster.status = WAIT_FOR_SYNC
            clusterRepository.save(cluster)
            if (!stackToSyncContainsCluster(stacksToSync, cluster)) {
                clustersToSync.add(cluster)
            }
        }
        return clustersToSync
    }

    private fun stackToSyncContainsCluster(stacksToSync: List<Stack>, cluster: Cluster): Boolean {
        val stackIds = stacksToSync.stream().map(Function<Stack, Long> { it.getId() }).collect(Collectors.toSet<Long>())
        return stackIds.contains(cluster.stack.id)
    }

    private fun setDeleteFailedStatus() {
        val stacksDeleteInProgress = stackRepository!!.findByStatuses(listOf<Status>(Status.DELETE_IN_PROGRESS))
        for (stack in stacksDeleteInProgress) {
            loggingStatusChange("Stack", stack.id, stack.status, Status.DELETE_FAILED)
            stack.status = Status.DELETE_FAILED
            stackRepository.save(stack)
        }
    }

    private fun terminateRunningFlows() {
        val runningFlows = flowLogRepository!!.findAllNonFinalized()
        val logMessage = "Terminating flow {}"
        for (flow in runningFlows) {
            LOGGER.info(logMessage, flow[0])
            flowLogService!!.terminate(flow[1] as Long, flow[0] as String)
        }
    }

    private fun loggingStatusChange(type: String, id: Long?, status: Status, deleteFailed: Status) {
        LOGGER.info("{} {} status is updated from {} to {} at CB start.", type, id, status, deleteFailed)
    }

    private fun triggerSyncs(stacksToSync: List<Stack>, clustersToSync: List<Cluster>) {
        for (stack in stacksToSync) {
            LOGGER.info("Triggering full sync on stack [name: {}, id: {}].", stack.name, stack.id)
            fireEvent(stack)
            flowManager!!.triggerFullSync(stack.id)
        }

        for (cluster in clustersToSync) {
            val stack = cluster.stack
            LOGGER.info("Triggering sync on cluster [name: {}, id: {}].", cluster.name, cluster.id)
            fireEvent(stack)
            flowManager!!.triggerClusterSync(stack.id)
        }
    }

    private fun fireEvent(stack: Stack) {
        eventService!!.fireCloudbreakEvent(stack.id, UPDATE_IN_PROGRESS.name,
                "Couldn't retrieve the cluster's status, starting to sync.")
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CloudbreakCleanupAction::class.java)
    }
}
