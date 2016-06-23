package com.sequenceiq.cloudbreak.cloud.event.credential

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus

class CredentialVerificationResult : CloudPlatformResult<CloudPlatformRequest<Any>> {

    val cloudCredentialStatus: CloudCredentialStatus

    constructor(request: CloudPlatformRequest<CredentialVerificationResult>, cloudCredentialStatus: CloudCredentialStatus) : super(request) {
        this.cloudCredentialStatus = cloudCredentialStatus
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<CredentialVerificationResult>) : super(statusReason, errorDetails, request) {
    }
}
