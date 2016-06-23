package com.sequenceiq.periscope.service.registry

class ServiceAddressResolvingException : Exception {
    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}
