package com.sequenceiq.periscope.api.exception

class RestClientInitializationException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(message: String) : super(message) {
    }
}


