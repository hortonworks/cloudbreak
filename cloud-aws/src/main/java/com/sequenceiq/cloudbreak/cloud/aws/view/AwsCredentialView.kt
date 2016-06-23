package com.sequenceiq.cloudbreak.cloud.aws.view

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

class AwsCredentialView(private val cloudCredential: CloudCredential) {

    val publicKey: String
        get() = cloudCredential.publicKey

    val name: String
        get() = cloudCredential.name

    val roleArn: String
        get() = cloudCredential.getParameter<String>("roleArn", String::class.java)

    val accessKey: String
        get() = cloudCredential.getParameter<String>("accessKey", String::class.java)

    val secretKey: String
        get() = cloudCredential.getParameter<String>("secretKey", String::class.java)

    val id: Long?
        get() = cloudCredential.id

}
