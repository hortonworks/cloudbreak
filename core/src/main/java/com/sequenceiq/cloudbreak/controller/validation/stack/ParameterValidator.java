package com.sequenceiq.cloudbreak.controller.validation.stack;

import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;

import java.util.List;
import java.util.Map;

public interface ParameterValidator {

    <O, E extends StackParamValidation> void validate(Map<String, O> parameters, List<E> paramsList);

    ValidatorType getValidatorType();
}
