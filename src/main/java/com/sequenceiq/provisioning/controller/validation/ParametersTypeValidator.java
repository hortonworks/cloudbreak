package com.sequenceiq.provisioning.controller.validation;

import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

@Component
public class ParametersTypeValidator extends AbstractParameterValidator {

    @Override
    public boolean validate(Map<String, String> parameters, ConstraintValidatorContext context, List<TemplateParam> paramList) {
        boolean valid = true;
        for (TemplateParam entry : paramList) {
            String param = parameters.get(entry.getName());
            if (param != null) {
                if (entry.getClazz().isEnum()) {
                    try {
                        entry.getClazz().getField(parameters.get(entry.getName()));
                    } catch (NoSuchFieldException e) {
                        addParameterConstraintViolation(context, entry.getName(), String.format("%s is not valid type.", entry.getName()));
                        valid = false;
                    }
                } else if (!parameters.get(entry.getName()).getClass().isAssignableFrom(entry.getClazz())) {
                    addParameterConstraintViolation(context, entry.getName(), String.format("%s is not valid type.", entry.getName()));
                    valid = false;
                }
            }
        }
        return valid;
    }

    @Override
    public ValidatorType getValidatorType() {
        return ValidatorType.CLASS;
    }


}
