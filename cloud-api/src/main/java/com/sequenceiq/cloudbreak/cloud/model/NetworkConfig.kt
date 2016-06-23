package com.sequenceiq.cloudbreak.cloud.model

class NetworkConfig private constructor() {

    init {
        throw IllegalStateException()
    }

    companion object {
        val OPEN_NETWORK = "0.0.0.0/0"
        val SUBNET_8 = "10.0.0.0/8"
        val SUBNET_16 = "10.0.0.0/16"
    }
}
