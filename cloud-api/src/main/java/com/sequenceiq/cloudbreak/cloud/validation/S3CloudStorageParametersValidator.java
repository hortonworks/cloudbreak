package com.sequenceiq.cloudbreak.cloud.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.cloud.model.storage.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.util.ValidatorUtil;

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
