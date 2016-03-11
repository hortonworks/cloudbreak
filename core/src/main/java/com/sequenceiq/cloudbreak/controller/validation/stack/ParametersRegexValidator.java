package com.sequenceiq.cloudbreak.controller.validation.stack;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.controller.BadRequestException;

@Component
public class ParametersRegexValidator implements ParameterValidator {

    @Override
    public <O, E extends StackParamValidation> void validate(Map<String, O> parameters, List<E> paramsList) {
        for (StackParamValidation param : paramsList) {
            if (param.getRegex().isPresent()
                    && parameters.containsKey(param.getName())
                    && !String.valueOf(parameters.get(param.getName())).matches(param.getRegex().get())) {
                throw new BadRequestException(String.format("%s does not match regex: %s.",
                        param.getName(), param.getRegex().get()));
            }
        }
    }

    @Override
    public ValidatorType getValidatorType() {
        return ValidatorType.REGEX;
    }
}
