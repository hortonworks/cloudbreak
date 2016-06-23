package com.sequenceiq.cloudbreak.orchestrator.model

class Node {
    var privateIp: String? = null
        private set
    var publicIp: String? = null
        private set
    var hostname: String? = null
        private set
    var hostGroup: String? = null
    val dataVolumes: Set<String>

    constructor(privateIp: String, publicIp: String, fqdn: String) : this(privateIp, publicIp) {
        this.hostname = fqdn
    }

    constructor(privateIp: String, publicIp: String) {
        this.privateIp = privateIp
        this.publicIp = publicIp
    }

    constructor(privateIp: String, publicIp: String, hostname: String, dataVolumes: Set<String>) {
        this.privateIp = privateIp
        this.publicIp = publicIp
        this.hostname = hostname
        this.dataVolumes = dataVolumes
    }

    override fun toString(): String {
        val sb = StringBuilder("Node{")
        sb.append("privateIp='").append(privateIp).append('\'')
        sb.append(", publicIp='").append(publicIp).append('\'')
        sb.append(", hostname='").append(hostname).append('\'')
        sb.append(", hostGroup='").append(hostGroup).append('\'')
        sb.append(", dataVolumes=").append(dataVolumes)
        sb.append('}')
        return sb.toString()
    }
}
