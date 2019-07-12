package com.sequenceiq.common.api.util;

import javax.validation.ConstraintValidatorContext;

public class ValidatorUtil {

    private ValidatorUtil() {
    }

    public static ConstraintValidatorContext addConstraintViolation(ConstraintValidatorContext context, String message, String propertyModel) {
        return context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyModel)
                .addConstraintViolation();
    }

    public static ConstraintValidatorContext addConstraintViolation(ConstraintValidatorContext context, String message) {
        return context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }

}
