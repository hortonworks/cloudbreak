package com.sequenceiq.cloudbreak.core.flow2.cluster.provision

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.StackService

@Component
class ClusterCreationFlowTriggerCondition : FlowTriggerCondition {

    @Inject
    private val stackService: StackService? = null

    override fun isFlowTriggerable(stackId: Long?): Boolean {
        val stack = stackService!!.getById(stackId)
        val result = stack.isAvailable && stack.cluster != null && stack.cluster.isRequested
        if (!result) {
            LOGGER.warn("Cluster creation cannot be triggered, because cluster is not in requested status or stack is not available.")
        }
        return result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterCreationFlowTriggerCondition::class.java)
    }
}
