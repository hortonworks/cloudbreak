package com.sequenceiq.cloudbreak.controller.validation.stack;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.controller.BadRequestException;

@Component
public class ParametersRequiredValidator implements ParameterValidator {

    @Override
    public <O, E extends StackParamValidation> void validate(Map<String, O> parameters, List<E> paramsList) {
        for (E param : paramsList) {
            if (Boolean.TRUE.equals(param.getRequired()) && !parameters.containsKey(param.getName())) {
                throw new BadRequestException(String.format("%s is required.", param.getName()));
            }
        }
    }

    @Override
    public ValidatorType getValidatorType() {
        return ValidatorType.REQUIRED;
    }

}
