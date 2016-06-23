package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

class GetPlatformVariantsRequest : CloudPlatformRequest<GetPlatformVariantsResult> {

    constructor() : super(null, null) {
    }

    constructor(cloudContext: CloudContext, cloudCredential: CloudCredential) : super(cloudContext, cloudCredential) {
    }

    //BEGIN GENERATED CODE
    override fun toString(): String {
        return "GetPlatformVariantsRequest{}"
    }
    //END GENERATED CODE
}
