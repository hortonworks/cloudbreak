package com.sequenceiq.common.api.util;

import jakarta.validation.ConstraintValidatorContext;

public class ValidatorUtil {

    private ValidatorUtil() {
    }

    public static ConstraintValidatorContext addConstraintViolation(ConstraintValidatorContext context, String message, String propertyModel) {
        ConstraintValidatorContext constraintValidatorContext;
        ConstraintValidatorContext.ConstraintViolationBuilder validationBuilder = context.buildConstraintViolationWithTemplate(message);
        if (propertyModel != null) {
            constraintValidatorContext = validationBuilder
                    .addPropertyNode(propertyModel)
                    .addConstraintViolation();
        } else {
            constraintValidatorContext = validationBuilder.addConstraintViolation();
        }
        return constraintValidatorContext;
    }

    public static ConstraintValidatorContext addConstraintViolation(ConstraintValidatorContext context, String message) {
        return addConstraintViolation(context, message, null);
    }
}
