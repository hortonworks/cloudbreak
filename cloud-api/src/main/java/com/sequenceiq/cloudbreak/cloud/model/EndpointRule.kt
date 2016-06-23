package com.sequenceiq.cloudbreak.cloud.model

class EndpointRule(val action: String, val remoteSubNet: String) {
    val description: String

    init {
        this.description = "Added by Cloudbreak"
    }

    enum class Action private constructor(val text: String) {
        PERMIT("permit"),
        DENY("deny")
    }

    companion object {

        val DENY_RULE = EndpointRule(Action.DENY.text, NetworkConfig.OPEN_NETWORK)
    }
}
