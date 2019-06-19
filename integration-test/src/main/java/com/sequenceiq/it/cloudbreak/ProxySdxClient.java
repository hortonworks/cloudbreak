package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.sdx.client.SdxServiceClient;

public class ProxySdxClient extends SdxServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxySdxClient.class);

    public ProxySdxClient(String sdxAddress, ConfigKey configKey) {
        super(sdxAddress, configKey);
    }

    @SuppressWarnings("unchecked")
    private <I> I createProxy(I obj, Class<I> clazz) {
        return new ProxyInstanceCreator(new ProxyHandler<>(obj, new BeforeAfterMessagingProxyExecutor())).createProxy(clazz);
    }
}
