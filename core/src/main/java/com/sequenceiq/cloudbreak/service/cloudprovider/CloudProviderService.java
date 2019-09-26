package com.sequenceiq.cloudbreak.service.cloudprovider;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.NoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;

@Service
public class CloudProviderService {

    private final CloudPlatformConnectors cloudPlatformConnectors;

    public CloudProviderService(CloudPlatformConnectors cloudPlatformConnectors) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
    }

    public ObjectStorageMetadataResponse getObjectStorageMetaData(ObjectStorageMetadataRequest request) {
        ObjectStorageConnector objectStorageConnector = getCloudConnector(request).objectStorage();
        return objectStorageConnector.getObjectStorageMetadata(request);
    }

    public NoSqlTableMetadataResponse getNoSqlTableMetaData(NoSqlTableMetadataRequest request) {
        NoSqlConnector noSqlConnector = getCloudConnector(request).noSql();
        return noSqlConnector.getNoSqlTableMetaData(request);
    }

    public NoSqlTableDeleteResponse deleteNoSqlTable(NoSqlTableDeleteRequest request) {
        NoSqlConnector noSqlConnector = getCloudConnector(request).noSql();
        return noSqlConnector.deleteNoSqlTable(request);
    }

    private CloudConnector<Object> getCloudConnector(CloudPlatformAware cloudPlatformAware) {
        return cloudPlatformConnectors.get(cloudPlatformAware.platform(), cloudPlatformAware.variant());
    }
}
