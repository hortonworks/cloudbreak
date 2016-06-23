package com.sequenceiq.cloudbreak.controller

class NotFoundException : RuntimeException {

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}
