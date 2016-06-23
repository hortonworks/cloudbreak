package com.sequenceiq.cloudbreak.orchestrator.salt.client.target

import java.util.stream.Collectors

class HostMatcher(private val addresses: List<String>) : Target<String> {

    override val target: String
        get() = addresses.stream().collect(Collectors.joining(","))

    override val type: String
        get() = "match"
}
