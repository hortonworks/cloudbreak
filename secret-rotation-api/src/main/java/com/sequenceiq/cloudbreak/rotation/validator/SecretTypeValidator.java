package com.sequenceiq.cloudbreak.rotation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretType;
import com.sequenceiq.common.api.util.ValidatorUtil;

public class SecretTypeValidator implements ConstraintValidator<ValidSecretType, String> {

    @Override
    public boolean isValid(String secret, ConstraintValidatorContext context) {
        try {
            SecretTypeConverter.mapSecretType(secret);
        } catch (Exception e) {
            ValidatorUtil.addConstraintViolation(context, e.getMessage());
            return false;
        }
        return true;
    }
}
