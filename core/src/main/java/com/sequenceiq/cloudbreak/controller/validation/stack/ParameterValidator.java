package com.sequenceiq.cloudbreak.controller.validation.stack;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;

public interface ParameterValidator {

    <O extends Object, E extends StackParamValidation> void validate(Map<String, O> parameters, List<E> paramsList);

    ValidatorType getValidatorType();
}
