package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;


public class GeneralSettingsValidator implements ConstraintValidator<ValidEnvironmentSettings, EnvironmentSettingsV4Request> {
    @Override
    public boolean isValid(EnvironmentSettingsV4Request value, ConstraintValidatorContext context) {
        return !StringUtils.isEmpty(value.getCredentialName()) || !StringUtils.isEmpty(value.getName());
    }
}
