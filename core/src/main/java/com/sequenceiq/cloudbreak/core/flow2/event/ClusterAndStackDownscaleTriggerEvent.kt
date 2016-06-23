package com.sequenceiq.cloudbreak.core.flow2.event

import com.sequenceiq.cloudbreak.common.type.ScalingType

class ClusterAndStackDownscaleTriggerEvent(selector: String, stackId: Long?, hostGroup: String, adjustment: Int?, val scalingType: ScalingType) : ClusterScaleTriggerEvent(selector, stackId, hostGroup, adjustment)
