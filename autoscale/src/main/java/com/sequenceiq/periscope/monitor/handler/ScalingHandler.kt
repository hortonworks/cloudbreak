package com.sequenceiq.periscope.monitor.handler

import java.lang.Math.ceil

import java.util.concurrent.ExecutorService

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

import com.sequenceiq.periscope.domain.BaseAlert
import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.domain.ScalingPolicy
import com.sequenceiq.periscope.log.MDCBuilder
import com.sequenceiq.periscope.monitor.event.ScalingEvent
import com.sequenceiq.periscope.service.ClusterService
import com.sequenceiq.periscope.utils.AmbariClientProvider
import com.sequenceiq.periscope.utils.ClusterUtils

@Component
class ScalingHandler : ApplicationListener<ScalingEvent> {

    @Autowired
    private val executorService: ExecutorService? = null
    @Autowired
    private val clusterService: ClusterService? = null
    @Autowired
    private val applicationContext: ApplicationContext? = null
    @Autowired
    private val ambariClientProvider: AmbariClientProvider? = null

    override fun onApplicationEvent(event: ScalingEvent) {
        val alert = event.alert
        val cluster = clusterService!!.find(alert.cluster.id)
        MDCBuilder.buildMdcContext(cluster)
        scale(cluster, alert.scalingPolicy)
    }

    private fun scale(cluster: Cluster, policy: ScalingPolicy) {
        val remainingTime = getRemainingCooldownTime(cluster)
        if (remainingTime <= 0) {
            val totalNodes = ClusterUtils.getTotalNodes(ambariClientProvider!!.createAmbariClient(cluster))
            val desiredNodeCount = getDesiredNodeCount(cluster, policy, totalNodes)
            if (totalNodes != desiredNodeCount) {
                val scalingRequest = applicationContext!!.getBean("ScalingRequest", cluster, policy, totalNodes, desiredNodeCount) as ScalingRequest
                executorService!!.execute(scalingRequest)
                cluster.setLastScalingActivityCurrent()
                clusterService!!.save(cluster)
            } else {
                LOGGER.info("No scaling activity required")
            }
        } else {
            LOGGER.info("Cluster cannot be scaled for {} min(s)",
                    ClusterUtils.TIME_FORMAT.format(remainingTime.toDouble() / ClusterUtils.MIN_IN_MS))
        }
    }

    private fun getRemainingCooldownTime(cluster: Cluster): Long {
        val coolDown = cluster.coolDown
        val lastScalingActivity = cluster.lastScalingActivity
        return if (lastScalingActivity == 0) 0 else coolDown * ClusterUtils.MIN_IN_MS - (System.currentTimeMillis() - lastScalingActivity)
    }

    private fun getDesiredNodeCount(cluster: Cluster, policy: ScalingPolicy, totalNodes: Int): Int {
        val scalingAdjustment = policy.scalingAdjustment
        val desiredNodeCount: Int
        when (policy.adjustmentType) {
            AdjustmentType.NODE_COUNT -> desiredNodeCount = totalNodes + scalingAdjustment
            AdjustmentType.PERCENTAGE -> desiredNodeCount = totalNodes + ceil(totalNodes * (scalingAdjustment.toDouble() / ClusterUtils.MAX_CAPACITY)).toInt()
            AdjustmentType.EXACT -> desiredNodeCount = policy.scalingAdjustment
            else -> desiredNodeCount = totalNodes
        }
        val minSize = cluster.minSize
        val maxSize = cluster.maxSize
        return if (desiredNodeCount < minSize) minSize else if (desiredNodeCount > maxSize) maxSize else desiredNodeCount
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ScalingHandler::class.java)
    }

}