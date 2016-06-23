package com.sequenceiq.periscope.service.configuration

enum class ConfigParam private constructor(private val key: String) {

    MR_FRAMEWORK_NAME("mapreduce.framework.name"),
    YARN_RM_ADDRESS("yarn.resourcemanager.address"),
    YARN_RM_WEB_ADDRESS("yarn.resourcemanager.webapp.address"),
    YARN_RM_SCHEDULER_ADDRESS("yarn.resourcemanager.scheduler.address"),
    YARN_SCHEDULER_ADDRESS("yarn.resourcemanager.scheduler.address"),
    RM_CONN_MAX_WAIT_MS("yarn.resourcemanager.connect.max-wait.ms"),
    RM_CONN_RETRY_INTERVAL_MS("yarn.resourcemanager.connect.retry-interval.ms");

    fun key(): String {
        return key
    }

}
