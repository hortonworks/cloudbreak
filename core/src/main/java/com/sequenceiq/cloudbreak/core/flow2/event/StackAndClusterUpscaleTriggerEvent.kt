package com.sequenceiq.cloudbreak.core.flow2.event

import com.sequenceiq.cloudbreak.common.type.ScalingType

class StackAndClusterUpscaleTriggerEvent(selector: String, stackId: Long?, instanceGroup: String, adjustment: Int?, val scalingType: ScalingType) : StackScaleTriggerEvent(selector, stackId, instanceGroup, adjustment)
