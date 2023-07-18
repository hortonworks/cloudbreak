package com.sequenceiq.cloudbreak.rotation.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;

public class MultiSecretTypeValidator implements ConstraintValidator<ValidMultiSecretType, String> {

    @Override
    public boolean isValid(String secret, ConstraintValidatorContext context) {
        return SecretTypeConverter.mapSecretType(secret).multiSecret();
    }
}
