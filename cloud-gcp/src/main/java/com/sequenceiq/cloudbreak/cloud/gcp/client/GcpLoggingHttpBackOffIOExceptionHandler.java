package com.sequenceiq.cloudbreak.cloud.gcp.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.util.BackOff;

public class GcpLoggingHttpBackOffIOExceptionHandler extends HttpBackOffIOExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpLoggingHttpBackOffIOExceptionHandler.class);

    /**
     * Constructs a new instance from a {@link BackOff}.
     *
     * @param backOff back-off policy
     */
    public GcpLoggingHttpBackOffIOExceptionHandler(BackOff backOff) {
        super(backOff);
    }

    @Override
    public boolean handleIOException(HttpRequest request, boolean supportsRetry) throws IOException {
        LOGGER.debug("Handle IO exception with retry-support: {}, for request with method: {} and URL: '{}'", supportsRetry, request.getRequestMethod(),
                request.getUrl());
        return super.handleIOException(request, supportsRetry);
    }
}
