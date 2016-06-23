package com.sequenceiq.it.cloudbreak.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "integrationtest")
class ITProps {
    var credentialNames: Map<String, String>? = null
    var defaultNetworks: Map<String, String>? = null
    var testSuites: Map<String, List<String>>? = null
    var testTypes: List<String>? = null
    var suiteFiles: List<String>? = null
    var defaultSecurityGroup: String? = null

    fun getCredentialName(cloudProvider: String): String {
        return credentialNames!![cloudProvider]
    }

    fun getDefaultNetwork(cloudProvider: String): String {
        return defaultNetworks!![cloudProvider]
    }

    fun getTestSuites(suitesKey: String): List<String> {
        return testSuites!![suitesKey]
    }
}
