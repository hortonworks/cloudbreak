package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint


class BlueprintProcessingException : RuntimeException {

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}
