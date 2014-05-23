package com.sequenceiq.provisioning.controller.validation;


import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

public abstract class AbstractParameterValidator implements ParameterValidator {
    @Override
    public abstract boolean validate(Map<String, String> parameters, ConstraintValidatorContext context, List<TemplateParam> paramsList);

    protected boolean validateClass(Map<String, String> parameters, ConstraintValidatorContext context, TemplateParam param) {
        boolean valid = true;
        if (param.getClazz().isEnum()) {
            try {
                param.getClazz().getField(parameters.get(param.getName()));
            } catch (NoSuchFieldException e) {
                addParameterConstraintViolation(context, param.getName(), String.format("%s is not valid type.", param.getName()));
                valid = false;
            }
        } else if (!parameters.get(param.getName()).getClass().isAssignableFrom(param.getClazz())) {
            addParameterConstraintViolation(context, param.getName(), String.format("%s is not valid type.", param.getName()));
            valid = false;
        }
        return valid;
    }

    protected void addParameterConstraintViolation(ConstraintValidatorContext context, String key, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("parameters")
                .addBeanNode().inIterable().atKey(key)
                .addConstraintViolation();
    }
}
