package com.sequenceiq.cloudbreak.cloud.gcp

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.common.type.ResourceType

class GcpResourceException : CloudConnectorException {
    constructor(cause: Throwable) : super(cause) {
    }

    constructor(message: String) : super(message) {
    }

    constructor(message: String, resourceType: ResourceType, name: String) : super(String.format("%s: [ resourceType: %s,  resourceName: %s ]", message, resourceType.name, name)) {
    }

    constructor(message: String, resourceType: ResourceType, name: String, cause: Throwable) : this(String.format("%s: [ resourceType: %s,  resourceName: %s ]", message, resourceType.name, name), cause) {
    }

    constructor(message: String, resourceType: ResourceType, name: String, stackId: Long?, operation: String) : super(String.format("%s: [ resourceType: %s,  resourceName: %s, stackId: %s, operation: %s ]", message, resourceType.name, name, stackId, operation)) {
    }

    constructor(message: String, resourceType: ResourceType, name: String, stackId: Long?, operation: String, cause: Throwable) : this(String.format("%s: [ resourceType: %s,  resourceName: %s, stackId: %s, operation: %s ]", message, resourceType.name, name, stackId, operation),
            cause) {
    }

    constructor(message: String, cause: Throwable) : super(message + "\n [ Cause message: " + cause.message + " ]\n", cause) {
    }
}
