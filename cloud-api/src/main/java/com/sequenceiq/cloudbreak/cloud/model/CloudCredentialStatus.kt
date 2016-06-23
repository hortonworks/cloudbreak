package com.sequenceiq.cloudbreak.cloud.model

class CloudCredentialStatus(val cloudCredential: CloudCredential, val status: CredentialStatus, val exception: Exception?, val statusReason: String?) {

    constructor(cloudResource: CloudCredential, status: CredentialStatus) : this(cloudResource, status, null, null) {
    }

    //BEGIN GENERATED CODE

    override fun toString(): String {
        return "CloudCredentialStatus{cloudCredential=$cloudCredential, status=$status, statusReason='$statusReason\'}"
    }


    //END GENERATED CODE


}
