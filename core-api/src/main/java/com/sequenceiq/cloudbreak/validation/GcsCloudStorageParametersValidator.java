package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsCloudStorageParameters;

public class GcsCloudStorageParametersValidator implements ConstraintValidator<ValidGcsCloudStorageParameters, GcsCloudStorageParameters> {

    private String failMessage = "";

    @Override
    public void initialize(ValidGcsCloudStorageParameters constraintAnnotation) {
    }

    @Override
    public boolean isValid(GcsCloudStorageParameters value, ConstraintValidatorContext context) {
        boolean result;
        if (!isServiceAccountEmailValid(value.getServiceAccountEmail())) {
            ValidatorUtil.addConstraintViolation(context, failMessage, "status");
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    private boolean isServiceAccountEmailValid(String serviceAccountEmail) {
        return isNotNull(serviceAccountEmail, "serviceAccountEmail should not be null!");
    }

    private boolean isNotNull(String content, String messageIfFails) {
        if (content == null) {
            failMessage = messageIfFails;
            return false;
        } else {
            return true;
        }
    }

}
