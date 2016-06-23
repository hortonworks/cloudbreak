package com.sequenceiq.cloudbreak.cloud.event.resource

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult

class TerminateStackResult : CloudPlatformResult<TerminateStackRequest<TerminateStackResult>> {

    constructor(request: TerminateStackRequest<TerminateStackResult>) : super(request) {
    }

    constructor(statusReason: String, errorDetails: Exception, request: TerminateStackRequest<TerminateStackResult>) : super(statusReason, errorDetails, request) {
    }
}
