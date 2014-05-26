package com.sequenceiq.provisioning.controller.validation;

import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

public class ParametersRegexValidator extends AbstractParameterValidator {
    
    @Override
    public boolean validate(Map<String, String> parameters, ConstraintValidatorContext context, List<TemplateParam> paramsList) {
        return false;
    }

    @Override
    public ValidatorType getValidatorType() {
        return ValidatorType.REGEX;
    }
}
