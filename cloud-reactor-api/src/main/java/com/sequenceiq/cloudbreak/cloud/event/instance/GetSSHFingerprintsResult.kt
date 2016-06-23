package com.sequenceiq.cloudbreak.cloud.event.instance

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult

class GetSSHFingerprintsResult : CloudPlatformResult<CloudPlatformRequest<Any>> {

    val sshFingerprints: Set<String>

    constructor(request: CloudPlatformRequest<*>, sshFingerprints: Set<String>) : super(request) {
        this.sshFingerprints = sshFingerprints
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
    }
}
