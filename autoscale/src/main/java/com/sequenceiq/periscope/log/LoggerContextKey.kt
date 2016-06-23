package com.sequenceiq.periscope.log

enum class LoggerContextKey private constructor(private val value: String) {

    OWNER_ID("owner"),
    RESOURCE_ID("resourceId"),
    CB_STACK_ID("cbStack");

    override fun toString(): String {
        return this.value
    }
}
