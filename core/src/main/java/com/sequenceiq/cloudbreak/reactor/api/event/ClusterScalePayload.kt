package com.sequenceiq.cloudbreak.reactor.api.event

class ClusterScalePayload(override val stackId: Long?, override val hostGroupName: String, override val scalingAdjustment: Int?) : ScalingAdjustmentPayload
