package com.sequenceiq.cloudbreak.converter.util;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.CloudStorageBase;

@Component
public class CloudStorageValidationUtil {

    public boolean isCloudStorageConfigured(CloudStorageBase cloudStorageRequest) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (cloudStorageRequest != null) {
            if (CollectionUtils.isEmpty(cloudStorageRequest.getLocations()) && CollectionUtils.isEmpty(cloudStorageRequest.getIdentities())) {
                return false;
            }
            validationBuilder.ifError(() -> CollectionUtils.isEmpty(cloudStorageRequest.getLocations()),
                    "'locations' in 'cloudStorage' must not be empty!");
            validationBuilder.ifError(() -> CollectionUtils.isEmpty(cloudStorageRequest.getIdentities()),
                    "'identities' in 'cloudStorage' must not be empty!");
            ValidationResult validationResult = validationBuilder.build();
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
            return true;
        } else {
            return false;
        }
    }
}
