package com.sequenceiq.common.api.cloudstorage.validation;

import java.util.List;
import java.util.Optional;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.cloudstorage.CloudStorageV1Base;

public class CloudStorageValidator implements ConstraintValidator<ValidCloudStorage, CloudStorageV1Base> {

    @Override
    public void initialize(ValidCloudStorage constraintAnnotation) {

    }

    @Override
    public boolean isValid(CloudStorageV1Base sb, ConstraintValidatorContext constraintValidatorContext) {
        long nonNullParams = List.of(Optional.ofNullable(sb.getS3()), Optional.ofNullable(sb.getAdls()), Optional.ofNullable(sb.getAdlsGen2()),
                Optional.ofNullable(sb.getGcs()), Optional.ofNullable(sb.getWasb())).stream()
                .filter(Optional::isPresent)
                .count();

        return exactlyOneParamFilled(nonNullParams);
    }

    private boolean exactlyOneParamFilled(long nonNullParams) {
        return nonNullParams == 1L;
    }
}
