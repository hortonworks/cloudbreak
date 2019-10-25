package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceApiKeyClient;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceUserCrnClientBuilder;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

public class EnvironmentClient extends MicroserviceClient {

    public static final String ENVIRONMENT_CLIENT = "ENVIRONMENT_CLIENT";

    private static com.sequenceiq.environment.client.EnvironmentClient singletonEnvironmentClient;

    private static EnvironmentInternalCrnClient singletonEnvironmentInternalCrnClient;

    private com.sequenceiq.environment.client.EnvironmentClient environmentClient;

    private EnvironmentInternalCrnClient environmentInternalCrnClient;

    EnvironmentClient(String newId) {
        super(newId);
    }

    EnvironmentClient() {
        this(ENVIRONMENT_CLIENT);
    }

    public static synchronized com.sequenceiq.environment.client.EnvironmentClient getSingletonEnvironmentClient() {
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
            singletonEnvironmentClient = new EnvironmentServiceApiKeyClient(
                    integrationTestContext.getContextParam(EnvironmentTest.ENVIRONMENT_SERVER_ROOT),
                    new ConfigKey(false, true, true))
                    .withKeys(integrationTestContext.getContextParam(EnvironmentTest.ACCESS_KEY),
                            integrationTestContext.getContextParam(EnvironmentTest.SECRET_KEY));
        }
        if (singletonEnvironmentInternalCrnClient == null) {
            singletonEnvironmentInternalCrnClient = createInternalEnvironmentClient(
                    integrationTestContext.getContextParam(EnvironmentTest.ENVIRONMENT_INTERNAL_SERVER_ROOT));
        }
        clientEntity.environmentClient = singletonEnvironmentClient;
        clientEntity.environmentInternalCrnClient = singletonEnvironmentInternalCrnClient;
    }

    public static synchronized EnvironmentClient createProxyEnvironmentClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        EnvironmentClient clientEntity = new EnvironmentClient();
        clientEntity.environmentClient = new EnvironmentServiceApiKeyClient(
                testParameter.get(EnvironmentTest.ENVIRONMENT_SERVER_ROOT),
                new ConfigKey(false, true, true))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        clientEntity.environmentInternalCrnClient = createInternalEnvironmentClient(
                testParameter.get(EnvironmentTest.ENVIRONMENT_INTERNAL_SERVER_ROOT));
        return clientEntity;
    }

    public static synchronized EnvironmentInternalCrnClient createInternalEnvironmentClient(String serverRoot) {
        EnvironmentServiceUserCrnClient userCrnClient = new EnvironmentServiceUserCrnClientBuilder(serverRoot)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        return new EnvironmentInternalCrnClient(userCrnClient, new InternalCrnBuilder(Crn.Service.IAM));
    }

    public com.sequenceiq.environment.client.EnvironmentClient getEnvironmentClient() {
        return environmentClient;
    }

    public void setEnvironmentClient(com.sequenceiq.environment.client.EnvironmentClient environmentClient) {
        this.environmentClient = environmentClient;
    }

    public EnvironmentInternalCrnClient getEnvironmentInternalCrnClient() {
        return environmentInternalCrnClient;
    }

    public void setEnvironmentInternalCrnClient(EnvironmentInternalCrnClient environmentInternalCrnClient) {
        this.environmentInternalCrnClient = environmentInternalCrnClient;
    }
}
