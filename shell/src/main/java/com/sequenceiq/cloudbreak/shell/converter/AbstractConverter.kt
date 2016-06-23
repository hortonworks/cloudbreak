package com.sequenceiq.cloudbreak.shell.converter

import java.lang.reflect.Constructor

import org.springframework.shell.core.Completion
import org.springframework.shell.core.Converter

import com.sequenceiq.cloudbreak.shell.completion.AbstractCompletion

abstract class AbstractConverter<T : AbstractCompletion> protected constructor() : Converter<T> {

    override fun convertFromText(value: String, clazz: Class<*>, optionContext: String): T? {
        try {
            val constructor = clazz.getDeclaredConstructor(String::class.java)
            return constructor.newInstance(value) as T
        } catch (e: Exception) {
            return null
        }

    }

    fun <E : Any> getAllPossibleValues(completions: MutableList<Completion>, values: Collection<E>): Boolean {
        for (value in values) {
            completions.add(Completion(value.toString()))
        }
        return true
    }

}