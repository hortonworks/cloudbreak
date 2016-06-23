package com.sequenceiq.cloudbreak.service.stack.connector

class OperationException : RuntimeException {

    constructor(cause: Throwable) : super(cause) {
    }

    constructor(message: String) : super(message) {
    }
}
