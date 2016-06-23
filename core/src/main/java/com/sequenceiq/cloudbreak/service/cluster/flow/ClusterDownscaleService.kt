package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.common.type.CloudConstants
import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.stack.StackService

@Service
class ClusterDownscaleService {

    @Inject
    private val stackService: StackService? = null

    @Inject
    private val clusterService: ClusterService? = null

    @Inject
    private val ambariDecommissioner: AmbariDecommissioner? = null

    @Throws(CloudbreakException::class)
    fun decommission(stackId: Long?, hostGroupName: String, scalingAdjustment: Int?): Set<String> {
        val stack = stackService!!.getById(stackId)
        MDCBuilder.buildMdcContext(stack)
        LOGGER.info("Start decommission")
        clusterService!!.updateClusterStatusByStackId(stack.id, UPDATE_IN_PROGRESS)
        return ambariDecommissioner!!.decommissionAmbariNodes(stack, hostGroupName, scalingAdjustment)
    }

    fun updateMetadata(stackId: Long?, hostNames: Set<String>) {
        val stack = stackService!!.getById(stackId)
        if (CloudConstants.BYOS != stack.cloudPlatform()) {
            MDCBuilder.buildMdcContext(stack)
            LOGGER.info("Start updating metadata")
            for (hostName in hostNames) {
                stackService.updateMetaDataStatus(stack.id, hostName, InstanceStatus.DECOMMISSIONED)
            }
        }
        clusterService!!.updateClusterStatusByStackId(stack.id, AVAILABLE)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ClusterDownscaleService::class.java)
    }
}