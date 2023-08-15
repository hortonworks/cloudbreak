package com.sequenceiq.cloudbreak.cloud.model.objectstorage;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.Range;

public class ObjectStoragePathLengthValidator implements ConstraintValidator<ValidObjectStoragePathLength, String> {

    private static final Integer MIN_LENGTH = 1;

    private static final Integer MAX_LENGTH = 99999;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && Range.between(MIN_LENGTH, MAX_LENGTH).contains(value.length());
    }
}
