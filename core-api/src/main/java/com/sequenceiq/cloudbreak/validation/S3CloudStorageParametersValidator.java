package com.sequenceiq.cloudbreak.validation;

import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class S3CloudStorageParametersValidator implements ConstraintValidator<ValidS3CloudStorageParameters, S3CloudStorageParameters> {

    @Override
    public void initialize(ValidS3CloudStorageParameters constraintAnnotation) {
    }

    @Override
    public boolean isValid(S3CloudStorageParameters value, ConstraintValidatorContext context) {
        boolean result;
        if (value.getInstanceProfile() == null) {
            ValidatorUtil.addConstraintViolation(context, "instancePrfile should not be null!", "status");
            result = false;
        } else {
            result = true;
        }
        return result;
    }

}
