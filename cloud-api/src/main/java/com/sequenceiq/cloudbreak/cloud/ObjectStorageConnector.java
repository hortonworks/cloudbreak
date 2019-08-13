package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;

/**
 * Object storage connectors.
 */
public interface ObjectStorageConnector extends CloudPlatformAware {

    ObjectStorageMetadataResponse getObjectStorageMetadata(ObjectStorageMetadataRequest request);

}
