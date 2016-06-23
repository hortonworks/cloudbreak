package com.sequenceiq.cloudbreak.cloud.task

interface Check<T> {

    fun completed(t: T): Boolean

    fun cancelled(): Boolean

}
