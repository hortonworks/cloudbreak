package com.sequenceiq.cloudbreak.core.bootstrap.service

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED

@Service
class StackDeletionBasedExitCriteria : ExitCriteria {

    override fun isExitNeeded(exitCriteriaModel: ExitCriteriaModel): Boolean {
        val model = exitCriteriaModel as StackDeletionBasedExitCriteriaModel
        LOGGER.debug("Check isExitNeeded for model: {}", model)
        val pollGroup = InMemoryStateStore.getStack(model.stackId)
        if (pollGroup != null && CANCELLED == pollGroup) {
            LOGGER.warn("Stack is getting terminated, polling is cancelled.")
            return true
        }
        return false
    }

    override fun exitMessage(): String {
        return "Stack is getting terminated, polling is cancelled."
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(StackDeletionBasedExitCriteria::class.java)
    }
}
