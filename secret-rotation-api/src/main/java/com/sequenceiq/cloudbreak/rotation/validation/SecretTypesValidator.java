package com.sequenceiq.cloudbreak.rotation.validation;

import java.util.Collections;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.util.ValidatorUtil;

public class SecretTypesValidator implements ConstraintValidator<ValidSecretTypes, List<String>> {

    @Override
    public boolean isValid(List<String> secrets, ConstraintValidatorContext context) {
        if (secrets.stream().anyMatch(secret -> Collections.frequency(secrets, secret) > 1)) {
            ValidatorUtil.addConstraintViolation(context, "There is at least one duplication in the request!");
            return false;
        }
        try {
            SecretTypeConverter.mapSecretTypes(secrets);
        } catch (Exception e) {
            ValidatorUtil.addConstraintViolation(context, e.getMessage());
            return false;
        }
        return true;
    }
}
