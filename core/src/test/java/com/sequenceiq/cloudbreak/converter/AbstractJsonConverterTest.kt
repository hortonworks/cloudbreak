package com.sequenceiq.cloudbreak.converter

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource

import com.sequenceiq.cloudbreak.TestException
import com.sequenceiq.cloudbreak.util.JsonUtil

abstract class AbstractJsonConverterTest<S> : AbstractConverterTest() {

    private val defaultSkippedFields = Arrays.asList("id", "owner", "account")

    fun getRequest(jsonFilePath: String): S {
        return readJsonFile(jsonFilePath, requestClass)
    }

    abstract val requestClass: Class<S>

    override fun assertAllFieldsNotNull(obj: Any) {
        super.assertAllFieldsNotNull(obj, defaultSkippedFields)
    }

    override fun assertAllFieldsNotNull(`object`: Any, skippedFields: List<String>) {
        val newFields = ArrayList<String>()
        newFields.addAll(defaultSkippedFields)
        newFields.addAll(skippedFields)
        super.assertAllFieldsNotNull(`object`, newFields)
    }

    private fun readJsonFile(jsonPath: String, clazz: Class<S>): S {
        try {
            val classPackage = javaClass.getPackage().getName().replace("\\.".toRegex(), "/")
            val resource = ClassPathResource(classPackage + "/" + jsonPath)
            val fileReader = BufferedReader(
                    FileReader(resource.file))
            return JsonUtil.readValue(fileReader, clazz)
        } catch (e: IOException) {
            throw TestException(e)
        }

    }
}
