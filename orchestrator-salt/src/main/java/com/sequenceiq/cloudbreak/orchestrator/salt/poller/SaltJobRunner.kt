package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType

interface SaltJobRunner {

    fun submit(saltConnector: SaltConnector): String

    var target: Set<String>

    var jid: JobId

    var jobState: JobState

    fun stateType(): StateType
}
