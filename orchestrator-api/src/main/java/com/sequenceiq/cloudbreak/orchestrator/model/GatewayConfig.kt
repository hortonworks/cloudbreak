package com.sequenceiq.cloudbreak.orchestrator.model

class GatewayConfig(val publicAddress: String, val privateAddress: String, val hostname: String?,
                    val gatewayPort: Int?, val certificateDir: String, val serverCert: String?, val clientCert: String?, val clientKey: String?) {

    constructor(publicAddress: String, privateAddress: String, gatewayPort: Int?, certificateDir: String) : this(publicAddress, privateAddress, null, gatewayPort, certificateDir, null, null, null) {
    }

    val gatewayUrl: String
        get() = String.format("https://%s:%d", publicAddress, gatewayPort)

    override fun toString(): String {
        return "GatewayConfig{"
        +"publicAddress='" + publicAddress + '\''
        +", privateAddress='" + privateAddress + '\''
        +", certificateDir='" + certificateDir + '\''
        +", serverCert='" + serverCert + '\''
        +", clientCert='" + clientCert + '\''
        +", clientKey='" + clientKey + '\''
        +'}'
    }
}
