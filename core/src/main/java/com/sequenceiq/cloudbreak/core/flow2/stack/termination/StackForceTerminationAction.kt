package com.sequenceiq.cloudbreak.core.flow2.stack.termination

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

@Component("StackForceTerminationAction")
class StackForceTerminationAction : StackTerminationAction() {
    override fun prepareExecution(payload: StackEvent, variables: MutableMap<Any, Any>) {
        variables.put("FORCEDTERMINATION", java.lang.Boolean.TRUE)
    }
}
