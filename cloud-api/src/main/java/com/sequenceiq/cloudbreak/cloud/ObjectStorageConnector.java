package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;

/**
 * Object storage connectors.
 */
public interface ObjectStorageConnector extends CloudPlatformAware {

    ObjectStorageMetadataResponse getObjectStorageMetadata(ObjectStorageMetadataRequest request);

    ObjectStorageValidateResponse validateObjectStorage(ObjectStorageValidateRequest request);
}
