package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem

class FileSystemConfigException : RuntimeException {

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

}
