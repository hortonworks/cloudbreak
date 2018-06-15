package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidatorContext;

public class ValidatorUtil {

    private ValidatorUtil() {
    }

    public static ConstraintValidatorContext addConstraintViolation(ConstraintValidatorContext context, String message, String propertyModel) {
        return context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyModel)
                .addConstraintViolation();
    }

}
