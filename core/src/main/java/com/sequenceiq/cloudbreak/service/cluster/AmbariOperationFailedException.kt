package com.sequenceiq.cloudbreak.service.cluster

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException

class AmbariOperationFailedException : CloudbreakServiceException {

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

}
