package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaltResponseFilter implements ClientResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltResponseFilter.class);

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext responseContext) throws IOException {
        LOGGER.info("[Salt Response]: url: {}, method: {}, statusCode: {}",
                clientRequestContext.getUri().getPath(),
                clientRequestContext.getMethod(),
                responseContext.getStatus());
    }
}
