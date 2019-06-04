package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.freeipa.api.FreeIpaApi;
import com.sequenceiq.freeipa.api.client.FreeIpaApiUserCrnClient;

public class ProxyFreeIPAClient extends FreeIpaApiUserCrnClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFreeIPAClient.class);

    public ProxyFreeIPAClient(String freeIPAAddress, ConfigKey configKey) {
        super(freeIPAAddress, configKey, FreeIpaApi.API_ROOT_CONTEXT);
    }

    @SuppressWarnings("unchecked")
    private <I> I createProxy(I obj, Class<I> clazz) {
        return new ProxyInstanceCreator(new ProxyHandler<>(obj, new BeforeAfterMessagingProxyExecutor())).createProxy(clazz);
    }
}
