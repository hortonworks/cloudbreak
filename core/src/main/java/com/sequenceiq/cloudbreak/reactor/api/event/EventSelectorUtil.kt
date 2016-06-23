package com.sequenceiq.cloudbreak.reactor.api.event

object EventSelectorUtil {

    fun selector(clazz: Class<Any>): String {
        return clazz.simpleName.toUpperCase()
    }

    fun failureSelector(clazz: Class<Any>): String {
        return clazz.simpleName.toUpperCase() + "_ERROR"
    }
}
