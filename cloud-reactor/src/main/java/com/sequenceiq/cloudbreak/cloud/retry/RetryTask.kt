package com.sequenceiq.cloudbreak.cloud.retry

interface RetryTask {
    @Throws(Exception::class)
    fun run()
}
