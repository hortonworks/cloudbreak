package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.s3.S3CloudStorageParameters;

public class S3CloudStorageParametersValidator implements ConstraintValidator<ValidS3CloudStorageParameters, S3CloudStorageParameters> {

    @Override
    public void initialize(ValidS3CloudStorageParameters constraintAnnotation) {
    }

    @Override
    public boolean isValid(S3CloudStorageParameters value, ConstraintValidatorContext context) {
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
