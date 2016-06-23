package com.sequenceiq.cloudbreak.cloud.retry

class RetryException : RuntimeException {

    constructor() {
    }

    constructor(message: String) : super(message) {
    }
}
