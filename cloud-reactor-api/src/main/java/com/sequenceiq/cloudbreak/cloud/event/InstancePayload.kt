package com.sequenceiq.cloudbreak.cloud.event

interface InstancePayload : Payload {
    val instanceId: String
}
