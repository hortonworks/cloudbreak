package com.sequenceiq.cloudbreak.controller.validation.stack

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.controller.BadRequestException

@Component
class ParametersRequiredValidator : ParameterValidator {

    override fun <O, E : StackParamValidation> validate(parameters: Map<String, O>, paramsList: List<E>) {
        for (param in paramsList) {
            if (java.lang.Boolean.TRUE == param.getRequired() && !parameters.containsKey(param.name)) {
                throw BadRequestException(String.format("%s is required.", param.name))
            }
        }
    }

    override val validatorType: ValidatorType
        get() = ValidatorType.REQUIRED

}
