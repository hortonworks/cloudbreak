package com.sequenceiq.cloudbreak.validation

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson

class UpdateStackRequestValidator : ConstraintValidator<ValidUpdateStackRequest, UpdateStackJson> {

    override fun initialize(constraintAnnotation: ValidUpdateStackRequest) {
        return
    }

    override fun isValid(value: UpdateStackJson, context: ConstraintValidatorContext): Boolean {
        var updateResources = 0
        if (value.status != null) {
            updateResources++
        }
        val instanceGroupAdjustment = value.instanceGroupAdjustment
        if (instanceGroupAdjustment != null) {
            updateResources++
            if (instanceGroupAdjustment.withClusterEvent!! && instanceGroupAdjustment.scalingAdjustment < 0) {
                addConstraintViolation(context,
                        "Invalid PUT request on this resource. Update event has to be upscale if you define withClusterEvent = 'true'.")
                return false
            }
        }

        if (updateResources != 1) {
            addConstraintViolation(context, "Invalid PUT request on this resource. 1 update request is allowed at a time.")
            return false
        }
        return true
    }

    private fun addConstraintViolation(context: ConstraintValidatorContext, message: String) {
        context.buildConstraintViolationWithTemplate(message).addPropertyNode("status").addConstraintViolation()
    }

}