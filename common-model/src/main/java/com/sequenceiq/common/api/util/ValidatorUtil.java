package com.sequenceiq.common.api.util;

import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

public class ValidatorUtil {

    private ValidatorUtil() {
    }

    public static ConstraintValidatorContext addConstraintViolation(ConstraintValidatorContext context, String message, String propertyModel) {
        message = avoidExpressionLanguageResolutionOnMessage(message);
        return context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyModel)
                .addConstraintViolation();
    }

    public static ConstraintValidatorContext addConstraintViolation(ConstraintValidatorContext context, String message) {
        message = avoidExpressionLanguageResolutionOnMessage(message);
        return context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }

    private static String avoidExpressionLanguageResolutionOnMessage(String message) {
        String result = message;
        if (StringUtils.isNotEmpty(message)) {
            result = message.replace("$", "");
        }
        return result;
    }

}
