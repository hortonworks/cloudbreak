package com.sequenceiq.common.api.cloudstorage.old.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageParameterBase;
import com.sequenceiq.common.api.util.ValidatorUtil;

public class S3CloudStorageParametersValidator implements ConstraintValidator<ValidS3CloudStorageParameters, S3CloudStorageParameterBase> {

    @Override
    public void initialize(ValidS3CloudStorageParameters constraintAnnotation) {
    }

    @Override
    public boolean isValid(S3CloudStorageParameterBase value, ConstraintValidatorContext context) {
        boolean result;
        if (value.getInstanceProfile() == null) {
            ValidatorUtil.addConstraintViolation(context, "instanceProfile should not be null!", "status");
            result = false;
        } else {
            result = true;
        }
        return result;
    }

}
