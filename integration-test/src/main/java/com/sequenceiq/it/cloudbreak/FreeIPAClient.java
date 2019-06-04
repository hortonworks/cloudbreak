package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.freeipa.api.client.FreeIpaApiUserCrnEndpoint;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

public class FreeIPAClient extends MicroserviceClient {
    public static final String FREEIPA_CLIENT = "FREEIPA_CLIENT";

    private static FreeIpaApiUserCrnEndpoint singletonFreeIpaClient;

    private static String crn;

    private FreeIpaApiUserCrnEndpoint freeIpaClient;

    FreeIPAClient(String newId) {
        super(newId);
    }

    FreeIPAClient() {
        this(FREEIPA_CLIENT);
    }

    public static synchronized FreeIpaApiUserCrnEndpoint getSingletonFreeIpaClient() {
        return singletonFreeIpaClient;
    }

    public static Function<IntegrationTestContext, FreeIPAClient> getTestContextFreeIPAClient(String key) {
        return testContext -> testContext.getContextParam(key, FreeIPAClient.class);
    }

    public static Function<IntegrationTestContext, FreeIPAClient> getTestContextFreeIPAClient() {
        return getTestContextFreeIPAClient(FREEIPA_CLIENT);
    }

    public static CloudbreakClient created() {
        CloudbreakClient client = new CloudbreakClient();
        client.setCreationStrategy(FreeIPAClient::createProxyFreeIPAClient);
        return client;
    }

    private static synchronized void createProxyFreeIPAClient(IntegrationTestContext integrationTestContext, Entity entity) {
        FreeIPAClient clientEntity = (FreeIPAClient) entity;
        if (singletonFreeIpaClient == null) {
            singletonFreeIpaClient = new ProxyFreeIPAClient(
                    integrationTestContext.getContextParam(FreeIPATest.FREEIPA_SERVER_ROOT),
                    new ConfigKey(false, true, true)).withCrn(integrationTestContext.getContextParam(FreeIPATest.USER_CRN));
        }
        clientEntity.freeIpaClient = singletonFreeIpaClient;
    }

    public static synchronized FreeIPAClient createProxyFreeIPAClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        FreeIPAClient clientEntity = new FreeIPAClient();
        clientEntity.freeIpaClient = new ProxyFreeIPAClient(
                testParameter.get(FreeIPATest.FREEIPA_SERVER_ROOT),
                new ConfigKey(false, true, true)).withCrn(cloudbreakUser.getToken());
        return clientEntity;
    }

    public FreeIpaApiUserCrnEndpoint getFreeIpaClient() {
        return freeIpaClient;
    }

    public void setFreeIpaClient(FreeIpaApiUserCrnEndpoint freeIpaClient) {
        this.freeIpaClient = freeIpaClient;
    }
}
