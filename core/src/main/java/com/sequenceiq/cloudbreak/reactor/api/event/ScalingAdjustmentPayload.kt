package com.sequenceiq.cloudbreak.reactor.api.event

interface ScalingAdjustmentPayload : HostGroupPayload {
    val scalingAdjustment: Int?
}
