package com.sequenceiq.cloudbreak.controller.validation.stack

import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation

interface ParameterValidator {

    fun <O : Any, E : StackParamValidation> validate(parameters: Map<String, O>, paramsList: List<E>)

    val validatorType: ValidatorType
}
