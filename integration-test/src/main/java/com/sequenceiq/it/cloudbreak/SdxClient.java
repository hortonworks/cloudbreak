package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.sdx.client.SdxServiceApiKeyClient;
import com.sequenceiq.sdx.client.SdxServiceApiKeyEndpoints;

public class SdxClient extends MicroserviceClient {
    public static final String SDX_CLIENT = "SDX_CLIENT";

    private SdxServiceApiKeyEndpoints sdxClient;

    private String name;

    SdxClient(String newId) {
        super(newId);
    }

    SdxClient() {
        this(SDX_CLIENT);
    }

    public static Function<IntegrationTestContext, SdxClient> getTestContextSdxClient(String key) {
        return testContext -> testContext.getContextParam(key, SdxClient.class);
    }

    public static Function<IntegrationTestContext, SdxClient> getTestContextSdxClient() {
        return getTestContextSdxClient(SDX_CLIENT);
    }

    public static synchronized SdxClient createProxySdxClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        SdxClient clientEntity = new SdxClient();
        clientEntity.setActing(cloudbreakUser);
        clientEntity.sdxClient = new SdxServiceApiKeyClient(
                testParameter.get(SdxTest.SDX_SERVER_ROOT),
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        return clientEntity;
    }

    public com.sequenceiq.sdx.client.SdxClient getSdxClient() {
        return sdxClient;
    }

    public void setSdxClient(SdxServiceApiKeyEndpoints sdxClient) {
        this.sdxClient = sdxClient;
    }
}

