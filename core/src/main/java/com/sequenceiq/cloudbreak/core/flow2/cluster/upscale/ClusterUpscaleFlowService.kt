package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS

import java.util.HashSet
import java.util.stream.Collectors

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.common.type.HostMetadataState
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.stack.StackService

@Component
class ClusterUpscaleFlowService {
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val clusterService: ClusterService? = null
    @Inject
    private val flowMessageService: FlowMessageService? = null
    @Inject
    private val emailSenderService: EmailSenderService? = null
    @Inject
    private val stackUpdater: StackUpdater? = null
    @Inject
    private val hostGroupService: HostGroupService? = null
    @Inject
    private val hostMetadataRepository: HostMetadataRepository? = null

    fun upscalingAmbari(stack: Stack) {
        clusterService!!.updateClusterStatusByStackId(stack.id, UPDATE_IN_PROGRESS, "Upscaling the cluster.")
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_SCALING_UP, UPDATE_IN_PROGRESS.name)
    }

    fun clusterUpscaleFinished(stack: Stack, hostgroupName: String) {
        val numOfFailedHosts = updateMetadata(stack, hostgroupName)
        val success = numOfFailedHosts == 0
        if (success) {
            LOGGER.info("Cluster upscaled successfully")
            clusterService!!.updateClusterStatusByStackId(stack.id, AVAILABLE)
            flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_SCALED_UP, AVAILABLE.name)
            if (stack.cluster.emailNeeded!!) {
                emailSenderService!!.sendUpscaleSuccessEmail(stack.cluster.owner, stack.ambariIp, stack.cluster.name)
                flowMessageService.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name)
            }
        } else {
            LOGGER.info("Cluster upscale failed. {} hosts failed to upscale", numOfFailedHosts)
            clusterService!!.updateClusterStatusByStackId(stack.id, UPDATE_FAILED)
            flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name, "added to",
                    String.format("Ambari upscale operation failed on %d node(s).", numOfFailedHosts))
        }
    }

    fun clusterUpscaleFailed(stack: Stack, errorDetails: Exception) {
        LOGGER.error("Error during Cluster upscale flow: " + errorDetails.message, errorDetails)
        clusterService!!.updateClusterStatusByStackId(stack.id, UPDATE_FAILED, errorDetails.message)
        stackUpdater!!.updateStackStatus(stack.id, AVAILABLE, String.format("New node(s) could not be added to the cluster: %s", errorDetails))
        flowMessageService!!.fireEventAndLog(stack.id, Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name, "added to", errorDetails)
    }

    private fun updateMetadata(stack: Stack, hostGroupName: String): Int {
        LOGGER.info("Start update metadata")
        val hostGroup = hostGroupService!!.getByClusterIdAndName(stack.cluster.id, hostGroupName)
        val hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.id)
        updateFailedHostMetaData(hostMetadata)
        var failedHosts = 0
        for (hostMeta in hostMetadata) {
            if ("BYOS" != stack.cloudPlatform() && hostGroup.constraint.instanceGroup != null) {
                stackService!!.updateMetaDataStatus(stack.id, hostMeta.hostName, InstanceStatus.REGISTERED)
            }
            hostGroupService.updateHostMetaDataStatus(hostMeta.id, HostMetadataState.HEALTHY)
            if (hostMeta.hostMetadataState === HostMetadataState.UNHEALTHY) {
                failedHosts++
            }
        }
        return failedHosts
    }

    private fun updateFailedHostMetaData(hostMetadata: Set<HostMetadata>) {
        val upscaleHostNames = getHostNames(hostMetadata)
        val successHosts = HashSet(upscaleHostNames)
        updateFailedHostMetaData(successHosts, hostMetadata)
    }

    private fun updateFailedHostMetaData(successHosts: Set<String>, hostMetadata: Set<HostMetadata>) {
        for (metaData in hostMetadata) {
            if (!successHosts.contains(metaData.hostName)) {
                metaData.hostMetadataState = HostMetadataState.UNHEALTHY
                hostMetadataRepository!!.save(metaData)
            }
        }
    }

    private fun getHostNames(hostMetadata: Set<HostMetadata>): List<String> {
        return hostMetadata.stream().map(Function<HostMetadata, String> { it.getHostName() }).collect(Collectors.toList<String>())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterUpscaleFlowService::class.java)
    }
}
