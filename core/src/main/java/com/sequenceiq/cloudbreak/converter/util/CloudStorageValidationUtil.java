package com.sequenceiq.cloudbreak.converter.util;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.CloudStorageBase;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.model.CloudIdentityType;

@Component
public class CloudStorageValidationUtil {

    public boolean isCloudStorageConfigured(CloudStorageBase cloudStorageRequest) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (cloudStorageRequest != null) {
            if (CollectionUtils.isEmpty(cloudStorageRequest.getLocations()) && CollectionUtils.isEmpty(cloudStorageRequest.getIdentities())) {
                return false;
            }
            if (!containsOnlyLogIdentity(cloudStorageRequest.getIdentities())) {
                validationBuilder.ifError(() -> CollectionUtils.isEmpty(cloudStorageRequest.getLocations())
                                && CollectionUtils.isEmpty(cloudStorageRequest.getIdentities()),
                        "Either 'locations' or 'identities' in 'cloudStorage' must not be empty!");
            }
            ValidationResult validationResult = validationBuilder.build();
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean containsOnlyLogIdentity(List<StorageIdentityBase> identities) {
        return CollectionUtils.isNotEmpty(identities) && identities.size() == 1
                && CloudIdentityType.LOG.equals(identities.get(0).getType());
    }
}
