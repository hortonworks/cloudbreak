package com.sequenceiq.it.cloudbreak.newway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.ConfigKey;

public class ProxyCloudbreakClient extends com.sequenceiq.cloudbreak.client.CloudbreakClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyCloudbreakClient.class);

    public ProxyCloudbreakClient(String cloudbreakAddress, String caasProtocol, String caasAddress, String refreshToken, ConfigKey configKey) {
        super(cloudbreakAddress, caasProtocol, caasAddress, refreshToken, configKey);
    }

    @Override
    protected <E> E getEndpoint(Class<E> clazz) {
        return createProxy(super.getEndpoint(clazz), clazz);
    }

    @SuppressWarnings("unchecked")
    private <I> I createProxy(I obj, Class<I> clazz) {
        return new ProxyInstanceCreator(new ProxyHandler<>(obj, new BeforeAfterMessagingProxyExecutor())).createProxy(clazz);
    }

}
