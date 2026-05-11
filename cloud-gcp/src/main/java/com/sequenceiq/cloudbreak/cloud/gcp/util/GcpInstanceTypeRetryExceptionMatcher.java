package com.sequenceiq.cloudbreak.cloud.gcp.util;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

public class GcpInstanceTypeRetryExceptionMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceTypeRetryExceptionMatcher.class);

    private static final String ZONE_RESOURCE_POOL_EXHAUSTED = "ZONE_RESOURCE_POOL_EXHAUSTED";

    private static final int UNAVAILABLE_STATUS_CODE = 429;

    private GcpInstanceTypeRetryExceptionMatcher() {
    }

    public static boolean isInstanceTypeNotSupported(GoogleJsonResponseException e) {
        if (e.getDetails() == null) {
            return false;
        }
        String message = e.getDetails().getMessage();
        int statusCode = e.getStatusCode();
        boolean unsupported = statusCode == HttpStatus.SC_BAD_REQUEST && message != null && message.contains("Invalid value for field 'resource.machineType'");
        boolean exhausted = (statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE || statusCode == UNAVAILABLE_STATUS_CODE)
                && message != null && (message.contains(ZONE_RESOURCE_POOL_EXHAUSTED));
        if (unsupported || exhausted) {
            LOGGER.info("Instance type related error during instance creation. status: {}, message: {}", statusCode, message);
            return true;
        }
        return false;
    }
}
