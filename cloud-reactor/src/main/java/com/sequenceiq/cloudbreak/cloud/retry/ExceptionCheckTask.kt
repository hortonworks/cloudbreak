package com.sequenceiq.cloudbreak.cloud.retry

interface ExceptionCheckTask {
    fun check(e: Exception): Boolean
}
