package com.sequenceiq.cloudbreak.client

class TokenUnavailableException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(message: String) : super(message) {
    }
}


