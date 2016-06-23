package com.sequenceiq.cloudbreak.orchestrator.security

class KerberosConfiguration(val masterKey: String, val user: String, val password: String) {
    companion object {

        val REALM = "NODE.DC1.CONSUL"
        val DOMAIN_REALM = "node.dc1.consul"
    }
}
