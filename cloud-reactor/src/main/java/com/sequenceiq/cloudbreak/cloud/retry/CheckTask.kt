package com.sequenceiq.cloudbreak.cloud.retry

interface CheckTask {
    fun check(): Boolean
}
