package com.sequenceiq.cloudbreak.controller.validation;

import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

@Component
public class ParametersRegexValidator extends AbstractParameterValidator {

    @Override
    public boolean validate(Map<String, Object> parameters, ConstraintValidatorContext context, List<TemplateParam> paramsList) {
        boolean valid = true;
        for (TemplateParam param : paramsList) {
            if (param.getRegex().isPresent()
                    && parameters.containsKey(param.getName())
                    && !String.valueOf(parameters.get(param.getName())).matches(param.getRegex().get())) {
                addParameterConstraintViolation(context, param.getName(), String.format("%s does not match regex: %s.",
                        param.getName(), param.getRegex().get()));
                valid = false;
            }
        }
        return valid;
    }

    @Override
    public ValidatorType getValidatorType() {
        return ValidatorType.REGEX;
    }
}
