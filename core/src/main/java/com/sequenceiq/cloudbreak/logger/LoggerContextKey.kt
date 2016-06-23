package com.sequenceiq.cloudbreak.logger

enum class LoggerContextKey private constructor(private val value: String) {

    OWNER_ID("owner"),
    RESOURCE_TYPE("resourceType"),
    RESOURCE_ID("resourceId"),
    RESOURCE_NAME("resourceName");

    override fun toString(): String {
        return this.value
    }
}
