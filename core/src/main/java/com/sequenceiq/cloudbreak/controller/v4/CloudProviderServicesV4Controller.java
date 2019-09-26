package com.sequenceiq.cloudbreak.controller.v4;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.service.cloudprovider.CloudProviderService;

@Controller
public class CloudProviderServicesV4Controller implements CloudProviderServicesV4Endopint {

    private final CloudProviderService cloudProviderService;

    public CloudProviderServicesV4Controller(CloudProviderService cloudProviderService) {
        this.cloudProviderService = cloudProviderService;
    }

    @Override
    public ObjectStorageMetadataResponse getObjectStorageMetaData(@Valid ObjectStorageMetadataRequest request) {
        return cloudProviderService.getObjectStorageMetaData(request);
    }

    @Override
    public NoSqlTableMetadataResponse getNoSqlTableMetaData(@Valid NoSqlTableMetadataRequest request) {
        return cloudProviderService.getNoSqlTableMetaData(request);
    }

    @Override
    public NoSqlTableDeleteResponse deleteNoSqlTable(@Valid NoSqlTableDeleteRequest request) {
        return cloudProviderService.deleteNoSqlTable(request);
    }
}
