package com.sequenceiq.cloudbreak.service.stack.flow

import java.lang.reflect.Field
import java.util.HashMap

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.domain.Encrypted

object ReflectionUtils {

    private val LOGGER = LoggerFactory.getLogger(ReflectionUtils::class.java)
    private var pbeStringCleanablePasswordEncryptor: PBEStringCleanablePasswordEncryptor? = null

    fun getDeclaredFields(`object`: Any): Map<String, Any> {
        val dynamicFields = HashMap<String, Any>()
        try {
            val superFields = `object`.javaClass.getSuperclass().getDeclaredFields()
            for (field in superFields) {
                if (!field.isAccessible()) {
                    field.setAccessible(true)
                }
                val name = field.getName()
                try {
                    dynamicFields.put(name, getValue(field, `object`))
                } catch (e: IllegalAccessException) {
                    LOGGER.error("Cannot retrieve field {} from class {}", name, `object`.javaClass.getName())
                }

            }
        } catch (e: Exception) {
            LOGGER.error("Cannot retrieve fields from superclass")
        }

        val fields = `object`.javaClass.getDeclaredFields()
        for (field in fields) {
            if (!field.isAccessible()) {
                field.setAccessible(true)
            }
            val name = field.getName()
            try {
                dynamicFields.put(name, getValue(field, `object`))
            } catch (e: IllegalAccessException) {
                LOGGER.error("Cannot retrieve field {} from class {}", name, `object`.javaClass.getName())
            }

        }

        return dynamicFields
    }

    fun setEncryptor(encryptor: PBEStringCleanablePasswordEncryptor) {
        pbeStringCleanablePasswordEncryptor = encryptor
    }

    @Throws(IllegalAccessException::class)
    private operator fun getValue(field: Field, `object`: Any): Any {
        val value = field.get(`object`)
        val annotation = field.getAnnotation<Encrypted>(Encrypted::class.java)
        return if (annotation == null) value else pbeStringCleanablePasswordEncryptor!!.decrypt(value as String)
    }

}
