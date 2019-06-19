package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.sdx.client.SdxServiceEndpoints;

public class SdxClient extends MicroserviceClient {
    public static final String SDX_CLIENT = "SDX_CLIENT";

    private static SdxServiceEndpoints singletonSdxClient;

    private static String crn;

    private SdxServiceEndpoints sdxClient;

    private String name;

    SdxClient(String newId) {
        super(newId);
    }

    SdxClient() {
        this(SDX_CLIENT);
    }

    public static synchronized SdxServiceEndpoints getSingletonSdxClient() {
        return singletonSdxClient;
    }

    public static Function<IntegrationTestContext, SdxClient> getTestContextSdxClient(String key) {
        return testContext -> testContext.getContextParam(key, SdxClient.class);
    }

    public static Function<IntegrationTestContext, SdxClient> getTestContextSdxClient() {
        return getTestContextSdxClient(SDX_CLIENT);
    }

    public static CloudbreakClient created() {
        CloudbreakClient client = new CloudbreakClient();
        client.setCreationStrategy(SdxClient::createProxySdxClient);
        return client;
    }

    private static synchronized void createProxySdxClient(IntegrationTestContext integrationTestContext, Entity entity) {
        SdxClient clientEntity = (SdxClient) entity;
        if (singletonSdxClient == null) {
            singletonSdxClient = new ProxySdxClient(
                    integrationTestContext.getContextParam(SdxTest.SDX_SERVER_ROOT),
                    new ConfigKey(false, true, true)).withCrn(integrationTestContext.getContextParam(SdxTest.USER_CRN));
        }
        clientEntity.sdxClient = singletonSdxClient;
    }

    public static synchronized SdxClient createProxySdxClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        SdxClient clientEntity = new SdxClient();
        clientEntity.sdxClient = new ProxySdxClient(
                testParameter.get(SdxTest.SDX_SERVER_ROOT),
                new ConfigKey(false, true, true)).withCrn(cloudbreakUser.getToken());
        return clientEntity;
    }

    public SdxServiceEndpoints getSdxClient() {
        return sdxClient;
    }

    public void setSdxClient(SdxServiceEndpoints sdxClient) {
        this.sdxClient = sdxClient;
    }
}

