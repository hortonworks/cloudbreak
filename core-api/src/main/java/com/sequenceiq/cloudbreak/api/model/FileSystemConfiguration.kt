package com.sequenceiq.cloudbreak.api.model

import java.util.HashMap

open class FileSystemConfiguration {
    private val dynamicProperties = HashMap<String, String>()

    fun getProperty(key: String): String {
        return dynamicProperties[key]
    }

    fun addProperty(key: String, value: String) {
        this.dynamicProperties.put(key, value)
    }

    companion object {

        val STORAGE_CONTAINER = "container"
        val RESOURCE_GROUP_NAME = "resourceGroupName"
        val ACCOUNT_NAME = "accountName"
        val ACCOUNT_KEY = "accountKey"
    }

}
