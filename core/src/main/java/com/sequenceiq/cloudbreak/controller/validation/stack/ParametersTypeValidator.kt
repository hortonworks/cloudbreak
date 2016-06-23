package com.sequenceiq.cloudbreak.controller.validation.stack

import java.lang.reflect.Field

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.controller.BadRequestException

@Component
class ParametersTypeValidator : ParameterValidator {

    override fun <O, E : StackParamValidation> validate(parameters: Map<String, O>, paramsList: List<E>) {
        for (entry in paramsList) {
            val param = parameters[entry.name]
            if (param != null) {
                if (entry.clazz.isEnum) {
                    try {
                        entry.clazz.getField(parameters[entry.name].toString())
                    } catch (e: NoSuchFieldException) {
                        throw BadRequestException(String.format("%s is not valid type. The valid fields are [%s]",
                                entry.name,
                                fieldList(entry.clazz.fields)))
                    }

                } else {
                    try {
                        entry.clazz.getConstructor(parameters[entry.name].javaClass).newInstance(parameters[entry.name])
                    } catch (e: Exception) {
                        try {
                            entry.clazz.getConstructor(String::class.java).newInstance(parameters[entry.name].toString())
                        } catch (ex: Exception) {
                            throw BadRequestException(ex.message)
                        }

                    }

                }
            }
        }
    }

    private fun fieldList(fields: Array<Field>): String {
        val sb = StringBuilder()
        for (field in fields) {
            sb.append(field.name)
            sb.append(SEPARATOR)
        }
        sb.replace(sb.toString().lastIndexOf(SEPARATOR), sb.toString().lastIndexOf(SEPARATOR) + 2, "")
        return sb.toString()
    }

    override val validatorType: ValidatorType
        get() = ValidatorType.CLASS

    companion object {

        private val SEPARATOR = ", "
    }


}
