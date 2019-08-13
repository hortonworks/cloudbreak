package com.sequenceiq.cloudbreak.service.cloudprovider;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;

@Service
public class CloudProviderService {

    private final CloudPlatformConnectors cloudPlatformConnectors;

    public CloudProviderService(CloudPlatformConnectors cloudPlatformConnectors) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
    }

    public ObjectStorageMetadataResponse getObjectStorageMetaData(ObjectStorageMetadataRequest request) {
        ObjectStorageConnector objectStorageConnector = cloudPlatformConnectors.get(Platform.platform(request.getCloudPlatform()),
                Variant.variant(request.getCloudPlatform())).objectStorage();
        return objectStorageConnector.getObjectStorageMetadata(request);
    }
}
