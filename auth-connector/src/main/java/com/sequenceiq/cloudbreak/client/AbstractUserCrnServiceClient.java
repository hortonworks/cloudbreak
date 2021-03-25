package com.sequenceiq.cloudbreak.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;

public abstract class AbstractUserCrnServiceClient<T extends AbstractUserCrnServiceEndpoint> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUserCrnServiceClient.class);

    private final Client client;

    private WebTarget webTarget;

    protected AbstractUserCrnServiceClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        client = RestClientUtil.get(configKey);
        webTarget = client.target(serviceAddress).path(apiRoot);
        LOGGER.info("{} has been created with token. cloudbreak: {}, configKey: {}", getClass().getName(), serviceAddress, configKey);
    }

    protected WebTarget getWebTarget() {
        return webTarget;
    }

    public void registerClientTracingFeature(ClientTracingFeature clientTracingFeature) {
        webTarget.register(clientTracingFeature);
    }

    public abstract T withCrn(String crn);
}
