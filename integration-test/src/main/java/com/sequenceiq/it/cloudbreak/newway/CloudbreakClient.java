package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.CloudbreakClient.CloudbreakClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.actor.CloudbreakUser;

public class CloudbreakClient extends Entity {
    public static final String CLOUDBREAK_CLIENT = "CLOUDBREAK_CLIENT";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakClient.class);

    private static com.sequenceiq.cloudbreak.client.CloudbreakClient singletonCloudbreakClient;

    private com.sequenceiq.cloudbreak.client.CloudbreakClient cloudbreakClient;

    private Long workspaceId;

    CloudbreakClient(String newId) {
        super(newId);
    }

    CloudbreakClient() {
        this(CLOUDBREAK_CLIENT);
    }

    public void setCloudbreakClient(com.sequenceiq.cloudbreak.client.CloudbreakClient cloudbreakClient) {
        this.cloudbreakClient = cloudbreakClient;
    }

    public com.sequenceiq.cloudbreak.client.CloudbreakClient getCloudbreakClient() {
        return cloudbreakClient;
    }

    public static Function<IntegrationTestContext, CloudbreakClient> getTestContextCloudbreakClient(String key) {
        return testContext -> testContext.getContextParam(key, CloudbreakClient.class);
    }

    public static Function<IntegrationTestContext, CloudbreakClient> getTestContextCloudbreakClient() {
        return getTestContextCloudbreakClient(CLOUDBREAK_CLIENT);
    }

    public static CloudbreakClient created() {
        CloudbreakClient client = new CloudbreakClient();
        client.setCreationStrategy(CloudbreakClient::createProxyCloudbreakClient);
        return client;
    }

    public static void newCloudbreakClientCreationStrategy(IntegrationTestContext integrationTestContext, Entity entity) {
        CloudbreakClient clientEntity = (CloudbreakClient) entity;
        com.sequenceiq.cloudbreak.client.CloudbreakClient client;
        client = new CloudbreakClientBuilder(
                integrationTestContext.getContextParam(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                integrationTestContext.getContextParam(CloudbreakTest.IDENTITY_URL),
                "cloudbreak_shell")
                .withCertificateValidation(false)
                .withDebug(true)
                .withCredential(integrationTestContext.getContextParam(CloudbreakTest.USER),
                        integrationTestContext.getContextParam(CloudbreakTest.PASSWORD))
                .withIgnorePreValidation(true)
                .build();
        clientEntity.cloudbreakClient = client;
    }

    private static synchronized void createProxyCloudbreakClient(IntegrationTestContext integrationTestContext, Entity entity) {
        CloudbreakClient clientEntity = (CloudbreakClient) entity;
        if (singletonCloudbreakClient == null) {
            singletonCloudbreakClient = new ProxyCloudbreakClient(
                    integrationTestContext.getContextParam(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                    integrationTestContext.getContextParam(CloudbreakTest.IDENTITY_URL),
                    integrationTestContext.getContextParam(CloudbreakTest.USER),
                    integrationTestContext.getContextParam(CloudbreakTest.PASSWORD),
                    "cloudbreak_shell",
                    new ConfigKey(false, true, true));
        }
        clientEntity.cloudbreakClient = singletonCloudbreakClient;
        String defaultWorkspace = integrationTestContext.getContextParam(CloudbreakTest.USER);
        clientEntity.workspaceId = singletonCloudbreakClient
                .workspaceV3Endpoint()
                .getByName(defaultWorkspace).getId();
    }

    public static synchronized CloudbreakClient createProxyCloudbreakClient(TestParameter testParameter) {
        CloudbreakClient clientEntity = new CloudbreakClient();
        if (singletonCloudbreakClient == null) {
            singletonCloudbreakClient = new ProxyCloudbreakClient(
                    testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                    testParameter.get(CloudbreakTest.IDENTITY_URL),
                    testParameter.get(CloudbreakTest.USER),
                    testParameter.get(CloudbreakTest.PASSWORD),
                    "cloudbreak_shell",
                    new ConfigKey(false, true, true));
        }
        clientEntity.cloudbreakClient = singletonCloudbreakClient;
        return clientEntity;
    }

    public static synchronized CloudbreakClient createProxyCloudbreakClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        CloudbreakClient clientEntity = new CloudbreakClient();
        ProxyCloudbreakClient cloudbreakClient = new ProxyCloudbreakClient(
                testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                testParameter.get(CloudbreakTest.IDENTITY_URL),
                cloudbreakUser.getUsername(),
                cloudbreakUser.getPassword(),
                "cloudbreak_shell",
                new ConfigKey(false, true, true));
        clientEntity.cloudbreakClient = cloudbreakClient;
        return clientEntity;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}

