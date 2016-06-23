package com.sequenceiq.cloudbreak.service.stack.flow

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException

class TerminationFailedException : CloudbreakServiceException {


    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }


}
