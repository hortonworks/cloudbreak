package com.sequenceiq.cloudbreak.cloud.openstack.view

import org.apache.commons.lang3.StringUtils.deleteWhitespace

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

class KeystoneCredentialView(authenticatedContext: AuthenticatedContext) {
    private val cloudCredential: CloudCredential
    val stackName: String

    init {
        this.stackName = authenticatedContext.cloudContext.name
        this.cloudCredential = authenticatedContext.cloudCredential
    }

    val keyPairName: String
        get() = String.format("%s-%s-%s-%s", CB_KEYPAIR_NAME, stackName, deleteWhitespace(name.toLowerCase()), cloudCredential.id)

    val name: String
        get() = cloudCredential.name

    val publicKey: String
        get() = cloudCredential.publicKey

    val userName: String
        get() = cloudCredential.getParameter<String>("userName", String::class.java)

    val password: String
        get() = cloudCredential.getParameter<String>("password", String::class.java)

    val tenantName: String
        get() = cloudCredential.getParameter<String>("tenantName", String::class.java)

    val endpoint: String
        get() = cloudCredential.getParameter<String>("endpoint", String::class.java)

    val userDomain: String
        get() = cloudCredential.getParameter<String>("userDomain", String::class.java)

    val projectName: String
        get() = cloudCredential.getParameter<String>("projectName", String::class.java)

    val projectDomain: String
        get() = cloudCredential.getParameter<String>("projectDomainName", String::class.java)

    val domainName: String
        get() = cloudCredential.getParameter<String>("domainName", String::class.java)

    val scope: String
        get() = cloudCredential.getParameter<String>("keystoneAuthScope", String::class.java)

    val version: String
        get() = cloudCredential.getParameter<String>("keystoneVersion", String::class.java)

    companion object {
        val CB_KEYSTONE_V2 = "cb-keystone-v2"
        val CB_KEYSTONE_V3_DEFAULT_SCOPE = "cb-keystone-v3-default-scope"
        val CB_KEYSTONE_V3_PROJECT_SCOPE = "cb-keystone-v3-project-scope"
        val CB_KEYSTONE_V3_DOMAIN_SCOPE = "cb-keystone-v3-domain-scope"

        private val CB_KEYPAIR_NAME = "cb"
    }
}
