package com.sequenceiq.cloudbreak.service.stack.flow

import com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED

import java.util.ArrayList
import java.util.Calendar
import java.util.Date

import javax.annotation.Resource
import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.FileSystemType
import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter

@Service
@Transactional
class TerminationService {

    @Inject
    private val connector: ServiceProviderConnectorAdapter? = null

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val stackUpdater: StackUpdater? = null

    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Inject
    private val clusterTerminationService: ClusterTerminationService? = null

    @Resource
    private val fileSystemConfigurators: Map<FileSystemType, FileSystemConfigurator<FileSystemConfiguration>>? = null

    fun finalizeTermination(stackId: Long?, force: Boolean) {
        val stack = stackRepository!!.findOneWithLists(stackId)
        try {
            val now = Date()
            val terminatedName = stack.name + DELIMITER + now.time
            val cluster = stack.cluster
            if (!force && cluster != null) {
                throw TerminationFailedException(String.format("There is a cluster installed on stack '%s', terminate it first!.", stackId))
            } else if (cluster != null) {
                clusterTerminationService!!.finalizeClusterTermination(cluster.id)
            }
            stack.credential = null
            stack.network = null
            stack.securityGroup = null
            stack.name = terminatedName
            terminateMetaDataInstances(stack)
            stackRepository.save(stack)
            stackUpdater!!.updateStackStatus(stackId, DELETE_COMPLETED, "Stack was terminated successfully.")
        } catch (ex: Exception) {
            LOGGER.error("Failed to terminate cluster infrastructure. Stack id {}", stack.id)
            throw TerminationFailedException(ex)
        }

    }

    private fun terminateMetaDataInstances(stack: Stack) {
        val instanceMetaDatas = ArrayList<InstanceMetaData>()
        for (metaData in stack.runningInstanceMetaData) {
            val timeInMillis = Calendar.getInstance().timeInMillis
            metaData.terminationDate = timeInMillis
            metaData.instanceStatus = InstanceStatus.TERMINATED
            instanceMetaDatas.add(metaData)
        }
        instanceMetaDataRepository!!.save(instanceMetaDatas)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TerminationService::class.java)
        private val DELIMITER = "_"
    }
}