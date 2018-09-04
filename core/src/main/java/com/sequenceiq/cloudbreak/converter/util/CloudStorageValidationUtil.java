package com.sequenceiq.cloudbreak.converter.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;

@Component
public class CloudStorageValidationUtil {

    public boolean isCloudStorageConfigured(CloudStorageRequest cloudStorageRequest) {
        return cloudStorageRequest != null
                && (cloudStorageRequest.getAdls() != null
                || cloudStorageRequest.getGcs() != null
                || cloudStorageRequest.getS3() != null
                || cloudStorageRequest.getWasb() != null
                || cloudStorageRequest.getAbfs() != null);
    }
}
