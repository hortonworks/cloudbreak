package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureResourceException;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;

import groovyx.net.http.HttpResponseException;

// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public abstract class AzureSimpleInstanceResourceBuilder implements ResourceBuilder<AzureDeleteContextObject> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AzureSimpleInstanceResourceBuilder.class);
    protected static final int POLLING_INTERVAL = 5000;
    protected static final int MAX_POLLING_ATTEMPTS = 60;
    protected static final int MAX_FAILURE_COUNT = 3;
    protected static final int NOT_FOUND = 404;
    protected static final String DESCRIPTION = "description";

    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.INSTANCE_RESOURCE;
    }

    protected void httpResponseExceptionHandler(HttpResponseException ex, String resourceName, String user) {
        if (ex.getStatusCode() != NOT_FOUND) {
            throw new AzureResourceException(ex.getResponse().getData().toString());
        } else {
            LOGGER.error(String.format("Azure resource not found with %s name for %s user.", resourceName, user));
        }
    }
}
