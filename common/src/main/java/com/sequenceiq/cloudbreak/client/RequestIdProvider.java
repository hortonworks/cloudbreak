package com.sequenceiq.cloudbreak.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;

public class RequestIdProvider {

    public static final String REQUEST_ID_HEADER = "x-cdp-request-id";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdProvider.class);

    private RequestIdProvider() {
    }

    public static String getOrGenerateRequestId() {
        String requestId = MDC.get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            LOGGER.debug("Generated requestId for request: {}", requestId);
        }
        return requestId;
    }

}
