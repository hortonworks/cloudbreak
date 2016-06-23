package com.sequenceiq.cloudbreak.reactor.api.event

import com.sequenceiq.cloudbreak.cloud.event.Payload

interface HostGroupPayload : Payload {
    val hostGroupName: String
}
