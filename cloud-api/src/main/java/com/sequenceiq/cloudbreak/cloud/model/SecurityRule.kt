package com.sequenceiq.cloudbreak.cloud.model

class SecurityRule(val cidr: String, val ports: Array<String>, val protocol: String) {

    override fun toString(): String {
        val sb = StringBuilder("SecurityRule{")
        sb.append("cidr='").append(cidr).append('\'')
        sb.append(", ports='").append(ports).append('\'')
        sb.append(", protocol='").append(protocol).append('\'')
        sb.append('}')
        return sb.toString()
    }
}
