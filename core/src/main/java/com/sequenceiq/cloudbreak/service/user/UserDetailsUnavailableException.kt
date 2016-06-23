package com.sequenceiq.cloudbreak.service.user

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException

class UserDetailsUnavailableException : CloudbreakServiceException {

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}
