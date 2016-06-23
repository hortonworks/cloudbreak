package com.sequenceiq.cloudbreak

class TestException : RuntimeException {
    constructor(cause: Throwable) : super(cause) {
    }

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}
