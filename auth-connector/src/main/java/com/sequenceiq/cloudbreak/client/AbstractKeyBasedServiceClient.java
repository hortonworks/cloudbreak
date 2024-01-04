package com.sequenceiq.cloudbreak.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractKeyBasedServiceClient<T extends AbstractKeyBasedServiceEndpoint> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUserCrnServiceClient.class);

    private final Client client;

    private WebTarget webTarget;

    protected AbstractKeyBasedServiceClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        client = RestClientUtil.get(configKey);
        webTarget = client.target(serviceAddress).path(apiRoot);
        LOGGER.info("{} has been created with token. cloudbreak: {}, configKey: {}", getClass().getName(), serviceAddress, configKey);
    }

    protected WebTarget getWebTarget() {
        return webTarget;
    }

    public abstract T withKeys(String accessKey, String privateKey);
}
