package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertFalse

import java.lang.reflect.Field

import com.google.common.collect.ObjectArrays
import com.sequenceiq.cloudbreak.TestException

open class AbstractConverterTest {
    open fun assertAllFieldsNotNull(obj: Any) {
        val fields = obtainFields(obj)
        for (field in fields) {
            assertFieldNotNull(obj, field)
        }
    }

    open fun assertAllFieldsNotNull(obj: Any, skippedFields: List<String>) {
        val fields = obtainFields(obj)
        for (field in fields) {
            if (!skippedFields.contains(field.name)) {
                assertFieldNotNull(obj, field)
            }
        }
    }

    private fun assertFieldNotNull(obj: Any, field: Field) {
        try {
            field.isAccessible = true
            assertFalse("Field '" + field.name + "' is null.", field.get(obj) == null)
        } catch (e: IllegalAccessException) {
            throw TestException(e.message)
        } catch (e: IllegalArgumentException) {
            throw TestException(e.message)
        }

    }

    private fun obtainFields(obj: Any): Array<Field> {
        val fields = obj.javaClass.getDeclaredFields()
        val parentFields = obj.javaClass.getSuperclass().getDeclaredFields()
        return ObjectArrays.concat<Field>(fields, parentFields, Field::class.java)
    }
}
