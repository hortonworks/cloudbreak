package com.sequenceiq.periscope.service.security

class UserDetailsUnavailableException : RuntimeException {

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}
