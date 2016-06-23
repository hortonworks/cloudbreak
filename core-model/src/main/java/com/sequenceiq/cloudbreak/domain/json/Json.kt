package com.sequenceiq.cloudbreak.domain.json

import java.io.IOException
import java.util.Collections

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.util.JsonUtil

class Json {

    var value: String? = null
        private set

    internal constructor(value: String) {
        this.value = value
    }

    @Throws(JsonProcessingException::class)
    constructor(value: Any) {
        this.value = JsonUtil.writeValueAsString(value)
    }

    @Throws(IOException::class)
    operator fun <T> get(valueType: Class<T>): T {
        return JsonUtil.readValue<T>(value, valueType)
    }

    val map: Map<String, Any>
        get() {
            try {
                if (value == null) {
                    return emptyMap<String, Any>()
                }
                return get<Map<Any, Any>>(Map<Any, Any>::class.java)
            } catch (e: IOException) {
                return emptyMap<String, Any>()
            }

        }

}
