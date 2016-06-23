package com.sequenceiq.cloudbreak.service

interface StatusCheckerTask<T> {

    fun checkStatus(t: T): Boolean

    fun handleTimeout(t: T)

    fun successMessage(t: T): String

    fun exitPolling(t: T): Boolean

    fun handleException(e: Exception)
}
