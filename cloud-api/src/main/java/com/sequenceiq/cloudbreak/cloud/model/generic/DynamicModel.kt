package com.sequenceiq.cloudbreak.cloud.model.generic

import java.util.Collections
import java.util.HashMap

/**
 * Generic mode to hold dynamic data, any data stored in the DynamicModel must be threadsafe in that sense that multiple threads might be
 * using it, but of course it is never used concurrently. In other words if you store anything in thread local then it might not be available
 * in a subsequent calls.

 */
open class DynamicModel {

    private val parameters: MutableMap<String, Any>

    constructor() {
        parameters = HashMap<String, Any>()
    }

    constructor(parameters: MutableMap<String, Any>) {
        this.parameters = parameters
    }

    @SuppressWarnings("unchecked")
    fun <T> getParameter(key: String, clazz: Class<T>): T {
        return parameters[key] as T
    }

    @SuppressWarnings("unchecked")
    fun <T> getParameter(clazz: Class<T>): T {
        return parameters[clazz.name] as T
    }

    fun getStringParameter(key: String): String {
        return getParameter(key, String::class.java)
    }

    fun putParameter(key: String, value: Any) {
        parameters.put(key, value)
    }

    fun putParameter(clazz: Class<Any>, value: Any) {
        putParameter(clazz.name, value)
    }

    fun getParameters(): Map<String, Any> {
        return Collections.unmodifiableMap(parameters)
    }
}
