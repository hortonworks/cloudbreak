package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.client.EnvironmentServiceClient;

public class ProxyEnvironmentClient extends EnvironmentServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyEnvironmentClient.class);

    public ProxyEnvironmentClient(String environmentAddress, ConfigKey configKey) {
        super(environmentAddress, configKey);
    }

    @SuppressWarnings("unchecked")
    private <I> I createProxy(I obj, Class<I> clazz) {
        return new ProxyInstanceCreator(new ProxyHandler<>(obj, new BeforeAfterMessagingProxyExecutor())).createProxy(clazz);
    }
}
