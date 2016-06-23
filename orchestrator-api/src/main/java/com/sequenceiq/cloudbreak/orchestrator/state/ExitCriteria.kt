package com.sequenceiq.cloudbreak.orchestrator.state

interface ExitCriteria {

    fun isExitNeeded(exitCriteriaModel: ExitCriteriaModel): Boolean

    fun exitMessage(): String
}
