package com.sequenceiq.periscope.monitor.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.periscope.api.model.ScalingStatus
import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.domain.ScalingPolicy
import com.sequenceiq.periscope.log.MDCBuilder
import com.sequenceiq.periscope.service.HistoryService

@Component("ScalingRequest")
@Scope("prototype")
class ScalingRequest(private val cluster: Cluster, private val policy: ScalingPolicy, private val totalNodes: Int, private val desiredNodeCount: Int) : Runnable {

    @Inject
    private val cloudbreakClient: CloudbreakClient? = null
    @Inject
    private val historyService: HistoryService? = null

    override fun run() {
        MDCBuilder.buildMdcContext(cluster)
        try {
            val scalingAdjustment = desiredNodeCount - totalNodes
            if (scalingAdjustment > 0) {
                scaleUp(scalingAdjustment, totalNodes)
            } else {
                scaleDown(scalingAdjustment, totalNodes)
            }
        } catch (e: Exception) {
            LOGGER.error("Cannot retrieve an oauth token from the identity server", e)
        }

    }

    private fun scaleUp(scalingAdjustment: Int, totalNodes: Int) {
        val hostGroup = policy.hostGroup
        val ambari = cluster.host
        val ambariAddressJson = AmbariAddressJson()
        ambariAddressJson.ambariAddress = ambari
        try {
            LOGGER.info("Sending request to add {} instance(s) and install services", scalingAdjustment)
            val stackId = cloudbreakClient!!.stackEndpoint().getStackForAmbari(ambariAddressJson).id
            val updateStackJson = UpdateStackJson()
            val instanceGroupAdjustmentJson = InstanceGroupAdjustmentJson()
            instanceGroupAdjustmentJson.withClusterEvent = true
            instanceGroupAdjustmentJson.scalingAdjustment = scalingAdjustment
            instanceGroupAdjustmentJson.instanceGroup = hostGroup
            updateStackJson.instanceGroupAdjustment = instanceGroupAdjustmentJson
            cloudbreakClient.stackEndpoint().put(stackId, updateStackJson)
            historyService!!.createEntry(ScalingStatus.SUCCESS, "Upscale successfully triggered", totalNodes, policy)
        } catch (e: Exception) {
            historyService!!.createEntry(ScalingStatus.FAILED, "Couldn't trigger upscaling due to: " + e.message, totalNodes, policy)
            LOGGER.error("Error adding nodes to cluster", e)
        }

    }

    private fun scaleDown(scalingAdjustment: Int, totalNodes: Int) {
        val hostGroup = policy.hostGroup
        val ambari = cluster.host
        val ambariAddressJson = AmbariAddressJson()
        ambariAddressJson.ambariAddress = ambari
        try {
            LOGGER.info("Sending request to remove {} node(s) from host group '{}'", scalingAdjustment, hostGroup)
            val stackId = cloudbreakClient!!.stackEndpoint().getStackForAmbari(ambariAddressJson).id
            val updateClusterJson = UpdateClusterJson()
            val hostGroupAdjustmentJson = HostGroupAdjustmentJson()
            hostGroupAdjustmentJson.scalingAdjustment = scalingAdjustment
            hostGroupAdjustmentJson.withStackUpdate = true
            hostGroupAdjustmentJson.hostGroup = hostGroup
            updateClusterJson.hostGroupAdjustment = hostGroupAdjustmentJson
            cloudbreakClient.clusterEndpoint().put(stackId, updateClusterJson)
            historyService!!.createEntry(ScalingStatus.SUCCESS, "Downscale successfully triggered", totalNodes, policy)
        } catch (e: Exception) {
            historyService!!.createEntry(ScalingStatus.FAILED, "Couldn't trigger downscaling due to: " + e.message, totalNodes, policy)
            LOGGER.error("Error removing nodes from the cluster", e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ScalingRequest::class.java)
    }

}
