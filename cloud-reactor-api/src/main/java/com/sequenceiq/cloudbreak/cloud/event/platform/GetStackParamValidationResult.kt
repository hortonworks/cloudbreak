package com.sequenceiq.cloudbreak.cloud.event.platform

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation

class GetStackParamValidationResult : CloudPlatformResult<CloudPlatformRequest<Any>> {
    val stackParamValidations: List<StackParamValidation>

    constructor(request: CloudPlatformRequest<*>, stackParamValidations: List<StackParamValidation>) : super(request) {
        this.stackParamValidations = stackParamValidations
    }

    constructor(statusReason: String, errorDetails: Exception, request: CloudPlatformRequest<*>) : super(statusReason, errorDetails, request) {
    }
}
