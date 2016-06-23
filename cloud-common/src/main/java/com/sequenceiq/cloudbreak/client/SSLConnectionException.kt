package com.sequenceiq.cloudbreak.client

class SSLConnectionException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(message: String) : super(message) {
    }
}


