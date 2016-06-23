package com.sequenceiq.cloudbreak.cloud.arm.view

class ArmPortView(val cidr: String, val port: String, val protocol: String) {

    val capitalProtocol: String
        get() = protocol.substring(0, 1).toUpperCase() + protocol.substring(1)
}
