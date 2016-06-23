package com.sequenceiq.cloudbreak.shell.transformer

import java.lang.reflect.Field
import java.util.Arrays
import java.util.HashMap

class ResponseTransformer<C : Collection<Any>> {

    fun transformToMap(responses: C, keyName: String, valueName: String): Map<String, String> {
        val transformed = HashMap<String, String>()

        for (`object` in responses) {
            var key = ""
            var value = ""
            var current: Class<*> = `object`.javaClass
            while (current.superclass != null) {

                for (field in current.declaredFields) {
                    field.isAccessible = true
                    try {
                        val o = field.get(`object`)
                        if (o != null) {
                            if (field.name == keyName) {
                                key = o.toString()
                            } else if (field.name == valueName) {
                                value = o.toString()
                            }
                        }
                    } catch (e: IllegalAccessException) {
                        value = "undefined"
                    }

                }
                current = current.superclass
            }
            transformed.put(key, value)
        }
        return transformed
    }

    fun transformObjectToStringMap(o: Any, vararg exclude: String): Map<String, String> {
        val result = HashMap<String, String>()
        var current: Class<*> = o.javaClass
        while (current.superclass != null) {
            for (field in current.declaredFields) {
                if (!Arrays.asList(*exclude).contains(field.name)) {
                    if (field.type.isAssignableFrom(Map<Any, Any>::class.java)) {
                        field.isAccessible = true
                        try {
                            val o1 = field.get(o) as Map<Any, Any>
                            for (objectObjectEntry in o1.entries) {
                                result.put(field.name + "." + objectObjectEntry.key,
                                        if (objectObjectEntry.value == null) "" else objectObjectEntry.value.toString())
                            }
                        } catch (e: IllegalAccessException) {
                            result.put(field.name, "undefined")
                        }

                    } else if (!field.type.isLocalClass) {
                        field.isAccessible = true
                        try {
                            result.put(field.name, if (field.get(o) == null) null else field.get(o).toString())
                        } catch (e: IllegalAccessException) {
                            result.put(field.name, "undefined")
                        }

                    } else {
                        for (field1 in field.type.declaredFields) {
                            field1.isAccessible = true
                            try {
                                result.put(field1.name, if (field1.get(o) == null) null else field1.get(o).toString())
                            } catch (e: IllegalAccessException) {
                                result.put(field.name + "." + field1.name, "undefined")
                            }

                        }
                    }
                }
            }
            current = current.superclass
        }
        return result
    }
}


