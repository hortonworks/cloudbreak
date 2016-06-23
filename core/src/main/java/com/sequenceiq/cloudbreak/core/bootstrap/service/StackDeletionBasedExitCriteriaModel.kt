package com.sequenceiq.cloudbreak.core.bootstrap.service

import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

class StackDeletionBasedExitCriteriaModel(val stackId: Long?) : ExitCriteriaModel() {

    override fun toString(): String {
        return "StackDeletionBasedExitCriteriaModel{"
        +"stackId=" + stackId + '}'
    }

    companion object {

        fun stackDeletionBasedExitCriteriaModel(stackId: Long?): ExitCriteriaModel {
            return StackDeletionBasedExitCriteriaModel(stackId)
        }
    }
}
