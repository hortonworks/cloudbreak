package com.sequenceiq.cloudbreak.cloud.arm.view

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

class ArmCredentialView(private val cloudCredential: CloudCredential) {

    val id: Long?
        get() = cloudCredential.id

    val publicKey: String
        get() = cloudCredential.publicKey

    val name: String
        get() = cloudCredential.name

    val subscriptionId: String
        get() = cloudCredential.getParameter<String>("subscriptionId", String::class.java)

    val accessKey: String
        get() = cloudCredential.getParameter<String>("accessKey", String::class.java)

    val secretKey: String
        get() = cloudCredential.getParameter<String>("secretKey", String::class.java)

    val tenantId: String
        get() = cloudCredential.getParameter<String>("tenantId", String::class.java)

    fun passwordAuthenticationRequired(): Boolean {
        return cloudCredential.publicKey.startsWith("Basic: ")
    }

    val password: String
        get() = cloudCredential.publicKey.replace("Basic: ".toRegex(), "")

    val loginUserName: String
        get() = cloudCredential.loginUserName

}
