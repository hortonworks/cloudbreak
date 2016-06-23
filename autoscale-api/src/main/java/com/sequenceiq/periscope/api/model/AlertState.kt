package com.sequenceiq.periscope.api.model

enum class AlertState private constructor(val value: String) {
    OK("OK"),
    WARN("WARNING"),
    CRITICAL("CRITICAL")
}
