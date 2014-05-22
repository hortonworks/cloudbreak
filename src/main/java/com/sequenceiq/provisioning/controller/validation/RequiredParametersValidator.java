package com.sequenceiq.provisioning.controller.validation;

import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

@Component
public class RequiredParametersValidator {

    public boolean validate(Map<String, String> parameters, ConstraintValidatorContext context, List<TemplateParam> requiredParams) {
        boolean valid = true;
        for (TemplateParam param : requiredParams) {
            if (!parameters.containsKey(param)) {
                addParameterConstraintViolation(context, param.getName(), String.format("%s is required.", param.getName()));
                valid = false;
            } else if (param.getClazz().isEnum()) {
                try {
                    param.getClazz().getField(parameters.get(param));
                } catch (NoSuchFieldException e) {
                    addParameterConstraintViolation(context, param.getName(), String.format("%s is not valid type.", param.getName()));
                    valid = false;
                }
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
