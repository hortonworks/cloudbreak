package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureStartStopContextObject;

import groovyx.net.http.HttpResponseException;

public abstract class AzureSimpleNetworkResourceBuilder implements
        ResourceBuilder<AzureProvisionContextObject, AzureDeleteContextObject, AzureDescribeContextObject, AzureStartStopContextObject> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AzureSimpleInstanceResourceBuilder.class);
    protected static final int NOT_FOUND = 404;
    protected static final String LOCATION = "location";
    protected static final String DESCRIPTION = "description";
    protected static final String AFFINITYGROUP = "affinityGroup";
    protected static final String ADDRESSPREFIX = "addressPrefix";
    protected static final String SUBNETADDRESSPREFIX = "subnetAddressPrefix";
    protected static final String SUBNETNAME = "subnetName";

    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.NETWORK_RESOURCE;
    }

    protected void httpResponseExceptionHandler(HttpResponseException ex, String resourceName, String user) {
        if (ex.getStatusCode() != NOT_FOUND) {
            throw new InternalServerException(ex.getMessage());
        } else {
            LOGGER.error(String.format("Azure resource not found with %s name for %s user.", resourceName, user));
        }
    }

    protected void waitForFinishing(AzureClient azureClient, String requestId) {
        boolean finished = azureClient.waitUntilComplete(requestId);
        if (!finished) {
            throw new InternalServerException("Azure resource timeout");
        }
    }

    @Override
    public Boolean start(AzureStartStopContextObject aSSCO, Resource resource) {
        return true;
    }

    @Override
    public Boolean stop(AzureStartStopContextObject aSSCO, Resource resource) {
        return true;
    }

    @Override
    public Boolean rollback(Resource resource, AzureDeleteContextObject azureDeleteContextObject) throws Exception {
        return delete(resource, azureDeleteContextObject);
    }
}
