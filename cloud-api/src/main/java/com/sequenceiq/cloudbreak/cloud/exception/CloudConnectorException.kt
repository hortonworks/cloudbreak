package com.sequenceiq.cloudbreak.cloud.exception

/**
 * Base [RuntimeException] for Cloud provider specific errors.
 */
open class CloudConnectorException : RuntimeException {

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }

    protected constructor(message: String, cause: Throwable, enableSuppression: Boolean, writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace) {
    }
}
