package com.sequenceiq.cloudbreak.cloud.yarn;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;

@Service
public class YarnObjectStorageConnector implements ObjectStorageConnector {

    @Override
    public ObjectStorageMetadataResponse getObjectStorageMetadata(ObjectStorageMetadataRequest request) {
        return ObjectStorageMetadataResponse.builder()
                .withStatus(ResponseStatus.OK)
                .build();
    }

    @Override
    public ObjectStorageValidateResponse validateObjectStorage(ObjectStorageValidateRequest request) {
        return ObjectStorageValidateResponse.builder()
                .withStatus(ResponseStatus.OK)
                .build();
    }

    @Override
    public Platform platform() {
        return YarnConstants.YARN_PLATFORM;
    }

    @Override
    public Variant variant() {
        return YarnConstants.YARN_VARIANT;
    }
}
