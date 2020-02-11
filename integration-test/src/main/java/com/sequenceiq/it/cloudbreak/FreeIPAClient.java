package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.freeipa.api.client.FreeIpaApiKeyClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

public class FreeIPAClient extends MicroserviceClient {
    public static final String FREEIPA_CLIENT = "FREEIPA_CLIENT";

    private static String crn;

    private com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient;

    FreeIPAClient(String newId) {
        super(newId);
    }

    FreeIPAClient() {
        this(FREEIPA_CLIENT);
    }

    public static Function<IntegrationTestContext, FreeIPAClient> getTestContextFreeIPAClient(String key) {
        return testContext -> testContext.getContextParam(key, FreeIPAClient.class);
    }

    public static Function<IntegrationTestContext, FreeIPAClient> getTestContextFreeIPAClient() {
        return getTestContextFreeIPAClient(FREEIPA_CLIENT);
    }

    public static synchronized FreeIPAClient createProxyFreeIPAClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        FreeIPAClient clientEntity = new FreeIPAClient();
        clientEntity.setActing(cloudbreakUser);
        clientEntity.freeIpaClient = new FreeIpaApiKeyClient(
                testParameter.get(FreeIPATest.FREEIPA_SERVER_ROOT),
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        return clientEntity;
    }

    public com.sequenceiq.freeipa.api.client.FreeIpaClient getFreeIpaClient() {
        return freeIpaClient;
    }

    public void setFreeIpaClient(com.sequenceiq.freeipa.api.client.FreeIpaClient freeIpaClient) {
        this.freeIpaClient = freeIpaClient;
    }
}
