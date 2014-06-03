package com.sequenceiq.cloudbreak.controller.validation;


import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

public abstract class AbstractParameterValidator implements ParameterValidator {

    @Override
    public abstract boolean validate(Map<String, Object> parameters, ConstraintValidatorContext context, List<TemplateParam> paramsList);

    protected void addParameterConstraintViolation(ConstraintValidatorContext context, String key, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("parameters")
                .addBeanNode().inIterable().atKey(key)
                .addConstraintViolation();
    }
}
