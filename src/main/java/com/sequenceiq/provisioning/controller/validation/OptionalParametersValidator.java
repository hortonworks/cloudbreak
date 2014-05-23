package com.sequenceiq.provisioning.controller.validation;

import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

@Component
public class OptionalParametersValidator extends AbstractParameterValidator {

    @Override
    public boolean validate(Map<String, String> parameters, ConstraintValidatorContext context, List<TemplateParam> paramList) {
        boolean valid = true;
        for (TemplateParam param : paramList) {
            if (parameters.containsKey(param.getName())) {
                if (!validateClass(parameters, context, param)) {
                    valid = false;
                }
            }
        }
        return valid;
    }

    @Override
    public ValidatorType getValidatorType() {
        return ValidatorType.OPTIONAL;
    }


}
