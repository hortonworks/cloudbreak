package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

class CheckPlatformVariantRequest(cloudContext: CloudContext, cloudCredential: CloudCredential) : CloudPlatformRequest<CheckPlatformVariantResult>(cloudContext, cloudCredential) {

    //BEGIN GENERATED CODE
    override fun toString(): String {
        return "CheckPlatformVariantRequest{}"
    }
    //END GENERATED CODE
}
