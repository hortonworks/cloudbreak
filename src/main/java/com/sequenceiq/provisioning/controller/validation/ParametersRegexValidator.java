package com.sequenceiq.provisioning.controller.validation;

import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

public class ParametersRegexValidator extends AbstractParameterValidator {

    @Override
    public boolean validate(Map<String, String> parameters, ConstraintValidatorContext context, List<TemplateParam> paramsList) {
        boolean valid = true;
        for (TemplateParam param : paramsList) {
            if (param.getRegex().isPresent()) {
                if (parameters.containsKey(param.getName())) {
                    if (parameters.get(param.getName()).matches(param.getRegex().get())){
                        addParameterConstraintViolation(context, param.getName(), String.format("%s is required.", param.getName()));
                        valid = false;
                    }
                } else {
                    valid = false;
                }
            }
        }
        return valid;
    }

    @Override
    public ValidatorType getValidatorType() {
        return ValidatorType.REGEX;
    }
}
