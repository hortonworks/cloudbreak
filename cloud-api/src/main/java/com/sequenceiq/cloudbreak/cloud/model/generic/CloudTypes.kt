package com.sequenceiq.cloudbreak.cloud.model.generic

abstract class CloudTypes<T>(private val types: Collection<T>, private val defaultType: T) {

    fun types(): Collection<T> {
        return types
    }

    fun defaultType(): T {
        return defaultType
    }
}
