package com.sequenceiq.cloudbreak.cloud.notification.model

/**
 * Used for confirming that a resource has been persisted
 */
class ResourcePersisted {

    val statusReason: String

    val exception: Exception

    constructor() {

    }

    constructor(statusReason: String, exception: Exception) {
        this.statusReason = statusReason
        this.exception = exception
    }
}
