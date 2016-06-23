package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest

class GetStackParamValidationRequest(cloudContext: CloudContext) : CloudPlatformRequest<GetStackParamValidationResult>(cloudContext, null) {

    //BEGIN GENERATED CODE
    override fun toString(): String {
        return "GetStackParamValidationRequest{" + cloudContext.toString() + "}"
    }
    //END GENERATED CODE
}
