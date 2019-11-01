package com.sequenceiq.cloudbreak.controller.v4;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableDeleteResponse;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.nosql.NoSqlTableMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.cloudprovider.CloudProviderService;

@Controller
public class CloudProviderServicesV4Controller implements CloudProviderServicesV4Endopint {

    private final CloudProviderService cloudProviderService;

    public CloudProviderServicesV4Controller(CloudProviderService cloudProviderService) {
        this.cloudProviderService = cloudProviderService;
    }

    @Override
    public ObjectStorageMetadataResponse getObjectStorageMetaData(@Valid ObjectStorageMetadataRequest request) {
        try {
            return cloudProviderService.getObjectStorageMetaData(request);
        } catch (CloudConnectorException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    public ObjectStorageValidateResponse validateObjectStorage(@Valid ObjectStorageValidateRequest request) {
        try {
            return cloudProviderService.validateObjectStorage(request);
        } catch (CloudConnectorException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    public NoSqlTableMetadataResponse getNoSqlTableMetaData(@Valid NoSqlTableMetadataRequest request) {
        try {
            return cloudProviderService.getNoSqlTableMetaData(request);
        } catch (CloudConnectorException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    public NoSqlTableDeleteResponse deleteNoSqlTable(@Valid NoSqlTableDeleteRequest request) {
        try {
            return cloudProviderService.deleteNoSqlTable(request);
        } catch (CloudConnectorException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
