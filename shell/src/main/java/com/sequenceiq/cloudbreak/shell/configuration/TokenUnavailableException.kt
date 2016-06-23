package com.sequenceiq.cloudbreak.shell.configuration

class TokenUnavailableException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(message: String) : super(message) {
    }
}

