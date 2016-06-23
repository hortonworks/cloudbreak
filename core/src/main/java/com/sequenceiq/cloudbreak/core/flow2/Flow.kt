package com.sequenceiq.cloudbreak.core.flow2

import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration

interface Flow {
    fun initialize()
    fun sendEvent(key: String, `object`: Any)
    val currentState: FlowState
    val flowId: String
    fun setFlowFailed()
    val isFlowFailed: Boolean
    val flowConfigClass: Class<out FlowConfiguration<FlowEvent>>
}
