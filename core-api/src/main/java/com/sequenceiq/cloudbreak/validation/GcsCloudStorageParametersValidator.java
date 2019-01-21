package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.gcs.GcsCloudStorageParametersV4;

public class GcsCloudStorageParametersValidator implements ConstraintValidator<ValidGcsCloudStorageParameters, GcsCloudStorageParametersV4> {

    private String failMessage = "";

    @Override
    public void initialize(ValidGcsCloudStorageParameters constraintAnnotation) {
    }

    @Override
    public boolean isValid(GcsCloudStorageParametersV4 value, ConstraintValidatorContext context) {
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
