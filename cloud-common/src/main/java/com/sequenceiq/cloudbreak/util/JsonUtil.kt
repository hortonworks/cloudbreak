package com.sequenceiq.cloudbreak.util

import java.io.IOException
import java.io.Reader

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

object JsonUtil {

    private val MAPPER = ObjectMapper()

    @Throws(IOException::class)
    fun <T> readValue(reader: Reader, valueType: Class<T>): T {
        return MAPPER.readValue(reader, valueType)
    }

    @Throws(IOException::class)
    fun <T> readValue(content: String, valueType: Class<T>): T {
        return MAPPER.readValue(content, valueType)
    }

    @Throws(JsonProcessingException::class)
    fun writeValueAsString(`object`: Any): String {
        return MAPPER.writeValueAsString(`object`)
    }

    @Throws(IOException::class)
    fun readTree(content: String): JsonNode {
        return MAPPER.readTree(content)
    }

    fun minify(content: String): String {
        try {
            return readTree(content).toString()
        } catch (e: IOException) {
            return "INVALID_JSON_CONTENT"
        }

    }

    @Throws(JsonProcessingException::class)
    fun <T> treeToValue(n: TreeNode, valueType: Class<T>): T {
        return MAPPER.treeToValue(n, valueType)
    }

}
