package com.sequenceiq.it

import java.util.HashMap

class IntegrationTestContext {

    private val contextParameters = HashMap<String, Any>()
    private val cleanUpParameters = HashMap<String, Any>()

    constructor() {
    }

    constructor(contextParameters: MutableMap<String, Any>) {
        this.contextParameters = contextParameters
    }

    fun getContextParam(paramKey: String): String {
        return getContextParam(paramKey, String::class.java)
    }

    fun <T> getContextParam(paramKey: String, clazz: Class<T>): T {
        val `val` = contextParameters[paramKey]
        if (`val` == null || clazz.isInstance(`val`)) {
            return clazz.cast(`val`)
        } else {
            throw IllegalArgumentException("Param value is not type of " + clazz)
        }
    }

    @JvmOverloads fun putContextParam(paramKey: String, paramValue: Any, cleanUp: Boolean = false) {
        contextParameters.put(paramKey, paramValue)
        if (cleanUp) {
            putCleanUpParam(paramKey, paramValue)
        }
    }

    fun putCleanUpParam(paramKey: String, paramValue: Any) {
        cleanUpParameters.put(paramKey, paramValue)
    }

    fun getCleanUpParameter(key: String): String {
        return getCleanUpParameter(key, String::class.java)
    }

    fun <T> getCleanUpParameter(key: String, clazz: Class<T>): T {
        val `val` = cleanUpParameters[key]
        if (`val` == null || clazz.isInstance(`val`)) {
            return clazz.cast(`val`)
        } else {
            throw IllegalArgumentException("Param value is not type of " + clazz)
        }
    }

    companion object {
        val IDENTITY_URL = "IDENTITY_URL"
        val AUTH_USER = "AUTH_USER"
        val AUTH_PASSWORD = "AUTH_PASSWORD"
    }
}
