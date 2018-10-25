package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;

public class GeneralSettingsValidator implements ConstraintValidator<ValidGeneralSettings, GeneralSettings> {
    @Override
    public boolean isValid(GeneralSettings value, ConstraintValidatorContext context) {
        return !StringUtils.isEmpty(value.getCredentialName()) || !StringUtils.isEmpty(value.getEnvironmentName());
    }
}
