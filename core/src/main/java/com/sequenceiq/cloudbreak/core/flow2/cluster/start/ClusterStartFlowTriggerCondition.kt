package com.sequenceiq.cloudbreak.core.flow2.cluster.start

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.StackService

@Component
class ClusterStartFlowTriggerCondition : FlowTriggerCondition {

    @Inject
    private val stackService: StackService? = null

    override fun isFlowTriggerable(stackId: Long?): Boolean {
        val stack = stackService!!.getById(stackId)
        val cluster = stack.cluster
        val result = cluster != null && cluster.isStartRequested
        if (!result) {
            LOGGER.warn("Cluster start cannot be triggered, because cluster is null or not in startRequested status")
        }
        return result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterStartFlowTriggerCondition::class.java)
    }
}
