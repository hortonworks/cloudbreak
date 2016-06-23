package com.sequenceiq.cloudbreak.core.flow2

interface FlowState {
    fun action(): Class<out AbstractAction<FlowState, FlowEvent, CommonContext, Payload>>
    fun name(): String
}
