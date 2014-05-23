package com.sequenceiq.provisioning.controller.validation;

import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

@Component
public class ParametersRequiredValidator extends AbstractParameterValidator {

    @Override
    public boolean validate(Map<String, String> parameters, ConstraintValidatorContext context, List<TemplateParam> paramList) {
        boolean valid = true;
        for (TemplateParam param : paramList) {
            if (Boolean.TRUE.equals(param.getRequired())) {
                if (!parameters.containsKey(param.getName())) {
                    addParameterConstraintViolation(context, param.getName(), String.format("%s is required.", param.getName()));
                    valid = false;
                }
            }
        }
        return valid;
    }

    @Override
    public ValidatorType getValidatorType() {
        return ValidatorType.REQUIRED;
    }

}
