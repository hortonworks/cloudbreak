package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidatorContext;

public class ValidatorUtil {

    private ValidatorUtil() {
    }

    public static void addConstraintViolation(ConstraintValidatorContext context, String message, String propertyModel) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyModel)
                .addConstraintViolation();
    }

    public static void addConstraintViolationAsStatus(ConstraintValidatorContext context, String message) {
        addConstraintViolation(context, message, "status");
    }

}
