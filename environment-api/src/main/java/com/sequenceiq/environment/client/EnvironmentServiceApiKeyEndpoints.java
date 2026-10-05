package com.sequenceiq.environment.client;

import jakarta.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceEndpoint;

public class EnvironmentServiceApiKeyEndpoints extends AbstractKeyBasedServiceEndpoint implements EnvironmentClient {

    protected EnvironmentServiceApiKeyEndpoints(WebTarget webTarget, String accessKey, String secretKey) {
        super(webTarget, accessKey, secretKey);
    }

}
