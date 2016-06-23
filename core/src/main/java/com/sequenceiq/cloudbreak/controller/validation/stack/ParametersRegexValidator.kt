package com.sequenceiq.cloudbreak.controller.validation.stack

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.controller.BadRequestException

@Component
class ParametersRegexValidator : ParameterValidator {

    override fun <O, E : StackParamValidation> validate(parameters: Map<String, O>, paramsList: List<E>) {
        for (param in paramsList) {
            if (param.regex.isPresent
                    && parameters.containsKey(param.name)
                    && !parameters[param.name].toString().matches(param.regex.get().toRegex())) {
                throw BadRequestException(String.format("%s does not match regex: %s.",
                        param.name, param.regex.get()))
            }
        }
    }

    override val validatorType: ValidatorType
        get() = ValidatorType.REGEX
}
