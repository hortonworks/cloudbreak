package com.sequenceiq.cloudbreak.rotation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.EnumUtils;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidMultiSecretType;

public class MultiSecretTypeValidator implements ConstraintValidator<ValidMultiSecretType, String> {

    @Override
    public boolean isValid(String secret, ConstraintValidatorContext context) {
        return EnumUtils.isValidEnum(MultiSecretType.class, secret);
    }
}
