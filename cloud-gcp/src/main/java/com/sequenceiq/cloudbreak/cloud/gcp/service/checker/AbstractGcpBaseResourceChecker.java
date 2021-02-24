package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractGcpBaseResourceChecker {

    public static final String OPERATION_ID = "opid";

    private static final String HTTP_CODE_KEY = "code";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpBaseResourceChecker.class);

    protected String checkException(GoogleJsonResponseException execute) {
        return execute.getDetails().getMessage();
    }

    protected CloudResource createNamedResource(ResourceType type, String name) {
        return new Builder().type(type).name(name).build();
    }

    protected void exceptionHandler(GoogleJsonResponseException ex, String name, ResourceType resourceType) {
        throwIfNull(ex, () -> new IllegalArgumentException("Unable to handle exception due to: " + GoogleJsonResponseException.class.getSimpleName()
                + " should not be null!"));
        GoogleJsonError jsonError = ex.getDetails();
        LOGGER.debug("{} contains the following {}: {}",
                GoogleJsonResponseException.class.getSimpleName(),  GoogleJsonError.class.getSimpleName(), jsonError != null ? jsonError.toString() : "null");
        if (jsonError != null) {
            if (jsonError.get(HTTP_CODE_KEY) != null && jsonError.get(HTTP_CODE_KEY).equals(HttpStatus.SC_NOT_FOUND)) {
                LOGGER.info("Resource {} not found: {}", resourceType, name);
            } else {
                LOGGER.warn("Unable to uncover the error code of {}! {}: {}",
                        GoogleJsonResponseException.class.getSimpleName(), GoogleJsonError.class.getSimpleName(), jsonError);
                throw new GcpResourceException(ex.getDetails().getMessage(), ex);
            }
        } else {
            String msg = String.format("Unable to uncover the detailed information of %s since %s does not contains the details!",
                    GoogleJsonResponseException.class.getSimpleName(), GoogleJsonError.class.getSimpleName());
            LOGGER.warn(msg, ex);
            throw new GcpResourceException(ex.getMessage(), ex);
        }
    }

}
