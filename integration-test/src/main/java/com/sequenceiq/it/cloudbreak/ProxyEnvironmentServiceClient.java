package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.client.EnvironmentServiceClient;

public class ProxyEnvironmentServiceClient extends EnvironmentServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyEnvironmentServiceClient.class);

    public ProxyEnvironmentServiceClient(String environmentServiceAddress, ConfigKey configKey) {
        super(environmentServiceAddress, configKey);
    }

    @SuppressWarnings("unchecked")
    private <I> I createProxy(I obj, Class<I> clazz) {
        return new ProxyInstanceCreator(new ProxyHandler<>(obj, new BeforeAfterMessagingProxyExecutor())).createProxy(clazz);
    }
}
