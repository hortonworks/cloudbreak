package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.client.EnvironmentServiceEndpoints;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

public class EnvironmentClient extends MicroserviceClient {

    public static final String ENVIRONMENT_CLIENT = "ENVIRONMENT_CLIENT";

    private static EnvironmentServiceEndpoints singletonEnvironmentClient;

    private static String crn;

    private EnvironmentServiceEndpoints environmentClient;

    EnvironmentClient(String newId) {
        super(newId);
    }

    EnvironmentClient() {
        this(ENVIRONMENT_CLIENT);
    }

    public static synchronized EnvironmentServiceEndpoints getSingletonEnvironmentClient() {
        return singletonEnvironmentClient;
    }

    public static Function<IntegrationTestContext, EnvironmentClient> getTestContextEnvironmentClient(String key) {
        return testContext -> testContext.getContextParam(key, EnvironmentClient.class);
    }

    public static Function<IntegrationTestContext, EnvironmentClient> getTestContextEnvironmentClient() {
        return getTestContextEnvironmentClient(ENVIRONMENT_CLIENT);
    }

    public static CloudbreakClient created() {
        CloudbreakClient client = new CloudbreakClient();
        client.setCreationStrategy(EnvironmentClient::createProxyEnvironmentClient);
        return client;
    }

    private static synchronized void createProxyEnvironmentClient(IntegrationTestContext integrationTestContext, Entity entity) {
        EnvironmentClient clientEntity = (EnvironmentClient) entity;
        if (singletonEnvironmentClient == null) {
            singletonEnvironmentClient = new ProxyEnvironmentClient(
                    integrationTestContext.getContextParam(EnvironmentTest.ENVIRONMENT_SERVER_ROOT),
                    new ConfigKey(false, true, true)).withCrn(integrationTestContext.getContextParam(EnvironmentTest.USER_CRN));
        }
        clientEntity.environmentClient = singletonEnvironmentClient;
    }

    public static synchronized EnvironmentClient createProxyEnvironmentClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        EnvironmentClient clientEntity = new EnvironmentClient();
        clientEntity.environmentClient = new ProxyEnvironmentClient(
                testParameter.get(EnvironmentTest.ENVIRONMENT_SERVER_ROOT),
                new ConfigKey(false, true, true)).withCrn(cloudbreakUser.getToken());
        return clientEntity;
    }

    public EnvironmentServiceEndpoints getEnvironmentClient() {
        return environmentClient;
    }

    public void setEnvironmentClient(EnvironmentServiceEndpoints environmentClient) {
        this.environmentClient = environmentClient;
    }
}
