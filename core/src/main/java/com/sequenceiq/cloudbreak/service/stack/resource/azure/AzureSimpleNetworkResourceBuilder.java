package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureResourceException;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureStartStopContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureUpdateContextObject;

import groovyx.net.http.HttpResponseException;

public abstract class AzureSimpleNetworkResourceBuilder implements
        ResourceBuilder<AzureProvisionContextObject, AzureDeleteContextObject, AzureStartStopContextObject, AzureUpdateContextObject> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AzureSimpleNetworkResourceBuilder.class);
    protected static final int POLLING_INTERVAL = 5000;
    protected static final int MAX_POLLING_ATTEMPTS = 60;
    protected static final int NOT_FOUND = 404;
    protected static final int NOT_EXIST = 400;
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

    protected void httpResponseExceptionHandler(HttpResponseException ex, String resourceName, String user, Stack stack) {
        if (ex.getStatusCode() != NOT_FOUND && ex.getStatusCode() != NOT_EXIST) {
            throw new AzureResourceException(ex.getResponse().getData().toString());
        } else {
            LOGGER.error(String.format("Azure resource not found with %s name for %s user.", resourceName, user));
        }
    }

    protected List<Resource> filterResourcesByType(List<Resource> resources, ResourceType resourceType) {
        List<Resource> resourcesTemp = new ArrayList<>();
        for (Resource resource : resources) {
            if (resourceType.equals(resource.getResourceType())) {
                resourcesTemp.add(resource);
            }
        }
        return resourcesTemp;
    }

    protected Exception checkException(Exception ex) {
        if (ex instanceof HttpResponseException) {
            return new AzureResourceException(((HttpResponseException) ex).getResponse().getData().toString());
        } else {
            return ex;
        }
    }

    @Override
    public void update(AzureUpdateContextObject updateContextObject) {
    }

    @Override
    public Boolean start(AzureStartStopContextObject aSSCO, Resource resource, String region) {
        return true;
    }

    @Override
    public Boolean stop(AzureStartStopContextObject aSSCO, Resource resource, String region) {
        return true;
    }

    @Override
    public Boolean rollback(Resource resource, AzureDeleteContextObject azureDeleteContextObject, String region) throws Exception {
        return delete(resource, azureDeleteContextObject, region);
    }
}
