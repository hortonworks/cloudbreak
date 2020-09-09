package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractGcpBaseResourceChecker {

    public static final String OPERATION_ID = "opid";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpBaseResourceChecker.class);

    protected String checkException(GoogleJsonResponseException execute) {
        return execute.getDetails().getMessage();
    }

    protected CloudResource createNamedResource(ResourceType type, String name) {
        return new Builder().type(type).name(name).build();
    }

    protected void exceptionHandler(GoogleJsonResponseException ex, String name, ResourceType resourceType) {
        if (ex.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND)) {
            LOGGER.debug("Resource {} not found: {}", resourceType, name);
        } else {
            throw new GcpResourceException(ex.getDetails().getMessage(), ex);
        }
    }
}
