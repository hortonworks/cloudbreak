package com.sequenceiq.common.api.cloudstorage.validation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.cloudstorage.CloudStorageV1Base;
import com.sequenceiq.common.api.cloudstorage.old.EfsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;

public class CloudStorageValidator implements ConstraintValidator<ValidCloudStorage, CloudStorageV1Base> {

    @Override
    public void initialize(ValidCloudStorage constraintAnnotation) {

    }

    @Override
    public boolean isValid(CloudStorageV1Base sb, ConstraintValidatorContext constraintValidatorContext) {
        List<Optional> nonNullParams = List.of(Optional.ofNullable(sb.getS3()), Optional.ofNullable(sb.getEfs()), Optional.ofNullable(sb.getAdls()),
                Optional.ofNullable(sb.getAdlsGen2()), Optional.ofNullable(sb.getGcs()), Optional.ofNullable(sb.getWasb())).stream()
                .filter(Optional::isPresent)
                .collect(Collectors.toList());

        return exactlyOneParamFilled(nonNullParams.size()) || validCloudStorageCombination(nonNullParams);
    }

    private boolean exactlyOneParamFilled(long nonNullParams) {
        return nonNullParams == 1L;
    }

    private boolean validCloudStorageCombination(List<Optional> nonNullParams) {
        long awsCount = nonNullParams.stream()
                .filter(nonNullParam -> nonNullParam.get() instanceof S3CloudStorageV1Parameters || nonNullParam.get() instanceof EfsCloudStorageV1Parameters)
                .count();

        return (awsCount >= 1 && awsCount <= 2) && (nonNullParams.size() == awsCount);
    }
}
