package com.sequenceiq.cloudbreak.controller.v4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
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
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.cloudprovider.CloudProviderService;

@Controller
@InternalOnly
public class CloudProviderServicesV4Controller implements CloudProviderServicesV4Endopint {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderServicesV4Controller.class);

    private final CloudProviderService cloudProviderService;

    public CloudProviderServicesV4Controller(CloudProviderService cloudProviderService) {
        this.cloudProviderService = cloudProviderService;
    }

    @Override
    @AccountIdNotNeeded
    public ObjectStorageMetadataResponse getObjectStorageMetaData(ObjectStorageMetadataRequest request) {
        try {
            return cloudProviderService.getObjectStorageMetaData(request);
        } catch (CloudConnectorException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    @AccountIdNotNeeded
    public ObjectStorageValidateResponse validateObjectStorage(ObjectStorageValidateRequest request) {
        try {
            LOGGER.info("Validate Object Storage request: {}", request);
            return cloudProviderService.validateObjectStorage(request);
        } catch (CloudConnectorException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    @AccountIdNotNeeded
    public NoSqlTableMetadataResponse getNoSqlTableMetaData(NoSqlTableMetadataRequest request) {
        try {
            return cloudProviderService.getNoSqlTableMetaData(request);
        } catch (CloudConnectorException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    @AccountIdNotNeeded
    public NoSqlTableDeleteResponse deleteNoSqlTable(NoSqlTableDeleteRequest request) {
        try {
            return cloudProviderService.deleteNoSqlTable(request);
        } catch (CloudConnectorException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
