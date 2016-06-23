package com.sequenceiq.cloudbreak.controller.validation.stack

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.StackRequest
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.service.stack.StackParameterService

@Component
class StackValidator {

    @Inject
    private val parameterValidators: List<ParameterValidator>? = null

    @Inject
    private val stackParameterService: StackParameterService? = null

    fun validate(stackRequest: StackRequest) {
        val stackParamValidations = stackParameterService!!.getStackParams(stackRequest)
        for (parameterValidator in parameterValidators!!) {
            parameterValidator.validate(stackRequest.parameters, stackParamValidations)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackValidator::class.java)
    }

}

