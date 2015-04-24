package com.sequenceiq.cloudbreak.controller.validation;

import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

public interface ParameterValidator {

    boolean validate(Map<String, Object> parameters, ConstraintValidatorContext context, List<TemplateParam> paramsList);

    ValidatorType getValidatorType();
}
