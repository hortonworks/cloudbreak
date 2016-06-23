package com.sequenceiq.periscope.service.security

class TlsConfigurationException : RuntimeException {

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}
