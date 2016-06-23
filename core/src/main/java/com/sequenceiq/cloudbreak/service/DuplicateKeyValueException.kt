package com.sequenceiq.cloudbreak.service


import com.sequenceiq.cloudbreak.common.type.APIResourceType

class DuplicateKeyValueException : RuntimeException {
    val resourceType: APIResourceType
    val value: String

    constructor(resourceType: APIResourceType, value: String) {
        this.resourceType = resourceType
        this.value = value
    }

    constructor(resourceType: APIResourceType, value: String, cause: Throwable) : super(cause) {
        this.resourceType = resourceType
        this.value = value
    }
}
