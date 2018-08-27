package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.client.ConfigKey;

public class ProxyCloudbreakClient extends com.sequenceiq.cloudbreak.client.CloudbreakClient {

    private static final String CAST_EXCEPTION_MESSAGE = "The provided ../newway.Entity is not castable to a StackEntity";

    private static final String BEFORE_MESSAGE = " Stack post request:\n";

    private static final String AFTER_MESSAGE = " Stack post response:\n";

    public ProxyCloudbreakClient(String cloudbreakAddress, String identityServerAddress, String user, String password, String clientId,
                                    ConfigKey configKey) {
        super(cloudbreakAddress, identityServerAddress, user, password, clientId, configKey);
    }

    protected ProxyCloudbreakClient(String cloudbreakAddress, String identityServerAddress, String secret, String clientId,
                                    ConfigKey configKey) {
        super(cloudbreakAddress, identityServerAddress, secret, clientId, configKey);
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
