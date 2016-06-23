package com.sequenceiq.cloudbreak.cloud.exception

/**
 * This [RuntimeException] is thrown in case an operation is not supported on a Cloud Platfrom.
 */
class CloudOperationNotSupportedException : CloudConnectorException {

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }

}