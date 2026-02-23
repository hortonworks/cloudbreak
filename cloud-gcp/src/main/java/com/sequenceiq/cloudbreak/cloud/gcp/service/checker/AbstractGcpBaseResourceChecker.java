package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractGcpBaseResourceChecker {

    public static final String OPERATION_ID = "opid";

    private static final String HTTP_CODE_KEY = "code";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGcpBaseResourceChecker.class);

    protected String checkException(GoogleJsonResponseException execute) {
        return execute.getDetails().getMessage();
    }

    protected CloudResource createNamedResource(ResourceType type, String name, String zone) {
        return CloudResource.builder()
                .withType(type)
                .withName(name)
                .withAvailabilityZone(zone)
                .withStatus(CommonStatus.REQUESTED)
                .build();
    }

    protected CloudResource createNamedResource(ResourceType type, String name, String zone, String group) {
        return CloudResource.builder()
                .withType(type)
                .withName(name)
                .withAvailabilityZone(zone)
                .withGroup(group)
                .withStatus(CommonStatus.REQUESTED)
                .build();
    }

    protected void exceptionHandler(GoogleJsonResponseException ex, String name, ResourceType resourceType) {
        GoogleJsonError jsonError = getGoogleJsonError(ex);
        if (jsonError != null) {
            if (jsonError.get(HTTP_CODE_KEY) != null && jsonError.get(HTTP_CODE_KEY).equals(HttpStatus.SC_NOT_FOUND)) {
                LOGGER.info("Resource {} not found: {}", resourceType, name);
            } else {
                throw extractedGcloudError(ex, jsonError);
            }
        } else {
            throw extractIfNoJson(ex);
        }
    }

    protected GcpResourceException exceptionHandlerWithThrow(GoogleJsonResponseException ex, String name, ResourceType resourceType) {
        GoogleJsonError jsonError = getGoogleJsonError(ex);
        if (jsonError != null) {
            if (jsonError.get(HTTP_CODE_KEY) != null && jsonError.get(HTTP_CODE_KEY).equals(HttpStatus.SC_NOT_FOUND)) {
                LOGGER.info("Resource {} not found: {}", resourceType, name);
                return new GcpResourceException(ex.getDetails().getMessage(), ex);
            } else {
                return extractedGcloudError(ex, jsonError);
            }
        } else {
            return extractIfNoJson(ex);
        }
    }

    private GcpResourceException extractedGcloudError(GoogleJsonResponseException ex, GoogleJsonError jsonError) {
        LOGGER.warn("Unable to cover the error code of {}! {}: {} with json error {}",
                GoogleJsonResponseException.class.getSimpleName(),
                GoogleJsonError.class.getSimpleName(),
                ex.getMessage(),
                jsonError);
        return new GcpResourceException(ex.getDetails().getMessage());
    }

    protected GoogleJsonError getGoogleJsonError(GoogleJsonResponseException ex) {
        throwIfNull(ex, () -> new IllegalArgumentException("Unable to handle exception due to: "
                + GoogleJsonResponseException.class.getSimpleName()
                + " should not be null!"));
        GoogleJsonError jsonError = ex.getDetails();
        LOGGER.debug("{} contains the following {}: {}",
                GoogleJsonResponseException.class.getSimpleName(), GoogleJsonError.class.getSimpleName(),
                jsonError != null ? jsonError.toString() : "null");
        return jsonError;
    }

    private GcpResourceException extractIfNoJson(GoogleJsonResponseException ex) {
        String msg = String.format("Unable to uncover the detailed information of %s since %s does not contains the details!",
                GoogleJsonResponseException.class.getSimpleName(), GoogleJsonError.class.getSimpleName());
        LOGGER.warn(msg, ex);
        return new GcpResourceException(ex.getMessage(), ex);
    }
}
