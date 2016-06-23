package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

class PlatformParameterRequest(cloudContext: CloudContext, cloudCredential: CloudCredential) : CloudPlatformRequest<PlatformParameterResult>(cloudContext, cloudCredential) {

    //BEGIN GENERATED CODE
    override fun toString(): String {
        return "PlatformParameterRequest{}"
    }
    //END GENERATED CODE
}
