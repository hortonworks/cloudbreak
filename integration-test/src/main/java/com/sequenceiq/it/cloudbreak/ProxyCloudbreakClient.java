package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class ProxyCloudbreakClient extends CloudbreakUserCrnClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyCloudbreakClient.class);

    private AutoscaleV4Endpoint autoscaleEndpoint;

    public ProxyCloudbreakClient(String cloudbreakAddress, ConfigKey configKey) {
        super(cloudbreakAddress, configKey, CoreApi.API_ROOT_CONTEXT);
    }

    @SuppressWarnings("unchecked")
    private <I> I createProxy(I obj, Class<I> clazz) {
        return new ProxyInstanceCreator(new ProxyHandler<>(obj, new BeforeAfterMessagingProxyExecutor())).createProxy(clazz);
    }
}
