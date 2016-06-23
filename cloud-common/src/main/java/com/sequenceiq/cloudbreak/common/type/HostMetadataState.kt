package com.sequenceiq.cloudbreak.common.type

enum class HostMetadataState private constructor(private val value: String) {
    CONTAINER_RUNNING("CONTAINER_RUNNING"),
    HEALTHY("HEALTY"),
    UNHEALTHY("UNHEALTHY")
}
