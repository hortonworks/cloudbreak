package com.sequenceiq.cloudbreak.cloud.retry

interface ErrorTask {
    fun run(e: Exception)
}
