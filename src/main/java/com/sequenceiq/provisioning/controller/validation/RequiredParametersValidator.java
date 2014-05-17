package com.sequenceiq.provisioning.controller.validation;

import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

@Component
public class RequiredParametersValidator {

    public boolean validate(Map<String, String> parameters, ConstraintValidatorContext context, List<String> requiredParams) {
        boolean valid = true;
        for (String param : requiredParams) {
            if (!parameters.containsKey(param)) {
                addParameterConstraintViolation(context, param, String.format("%s is required.", param));
                valid = false;
            }
        }
        return valid;
    }

    private void addParameterConstraintViolation(ConstraintValidatorContext context, String key, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("parameters")
                .addBeanNode().inIterable().atKey(key)
                .addConstraintViolation();
    }

}
