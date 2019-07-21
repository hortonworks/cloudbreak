package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.client.CloudbreakApiKeyClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

public class CloudbreakClient extends MicroserviceClient {
    public static final String CLOUDBREAK_CLIENT = "CLOUDBREAK_CLIENT";

    private static com.sequenceiq.cloudbreak.client.CloudbreakClient singletonCloudbreakClient;

    private static String crn;

    private com.sequenceiq.cloudbreak.client.CloudbreakClient cloudbreakClient;

    private Long workspaceId;

    CloudbreakClient(String newId) {
        super(newId);
    }

    CloudbreakClient() {
        this(CLOUDBREAK_CLIENT);
    }

    public static synchronized com.sequenceiq.cloudbreak.client.CloudbreakClient getSingletonCloudbreakClient() {
        return singletonCloudbreakClient;
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

    private static synchronized void createProxyCloudbreakClient(IntegrationTestContext integrationTestContext, Entity entity) {
        CloudbreakClient clientEntity = (CloudbreakClient) entity;
        if (singletonCloudbreakClient == null) {
            singletonCloudbreakClient = new CloudbreakApiKeyClient(
                    integrationTestContext.getContextParam(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                    new ConfigKey(false, true, true))
                    .withKeys(integrationTestContext.getContextParam(CloudbreakTest.ACCESS_KEY),
                            integrationTestContext.getContextParam(CloudbreakTest.SECRET_KEY));
        }
        clientEntity.cloudbreakClient = singletonCloudbreakClient;
    }

    public static synchronized CloudbreakClient createProxyCloudbreakClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        CloudbreakClient clientEntity = new CloudbreakClient();
        clientEntity.cloudbreakClient = new CloudbreakApiKeyClient(
                testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                new ConfigKey(false, true, true))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        return clientEntity;
    }

    public com.sequenceiq.cloudbreak.client.CloudbreakClient getCloudbreakClient() {
        return cloudbreakClient;
    }

    public void setCloudbreakClient(com.sequenceiq.cloudbreak.client.CloudbreakClient cloudbreakClient) {
        this.cloudbreakClient = cloudbreakClient;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}

