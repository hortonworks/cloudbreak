package com.sequenceiq.cloudbreak.converter.util;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.CloudStorageBase;

@Component
public class CloudStorageValidationUtil {

    public boolean isCloudStorageConfigured(CloudStorageBase cloudStorageRequest) {
        return cloudStorageRequest != null
                && (CollectionUtils.isNotEmpty(cloudStorageRequest.getIdentities())
                || CollectionUtils.isNotEmpty(cloudStorageRequest.getLocations()));
    }
}
