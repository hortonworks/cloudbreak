package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.auth.uaa.IdentityClient;
import com.sequenceiq.cloudbreak.restclient.ConfigKey;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.actor.CloudbreakUser;

public class CloudbreakClient extends Entity {
    public static final String CLOUDBREAK_CLIENT = "CLOUDBREAK_CLIENT";

    private static com.sequenceiq.cloudbreak.client.CloudbreakClient singletonCloudbreakClient;

    private com.sequenceiq.cloudbreak.client.CloudbreakClient cloudbreakClient;

    private IdentityClient identityClient;

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

    public static com.sequenceiq.cloudbreak.client.CloudbreakClient getSingletonCloudbreakClient() {
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
            singletonCloudbreakClient = new ProxyCloudbreakClient(
                    integrationTestContext.getContextParam(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                    integrationTestContext.getContextParam(CloudbreakTest.CAAS_PROTOCOL),
                    integrationTestContext.getContextParam(CloudbreakTest.CAAS_ADDRESS),
                    integrationTestContext.getContextParam(CloudbreakTest.REFRESH_TOKEN),
                    new ConfigKey(false, true, true),
                    integrationTestContext.getContextParam(CloudbreakTest.IDENTITY_URL),
                    integrationTestContext.getContextParam(CloudbreakTest.AUTOSCALE_CLIENT_ID),
                    integrationTestContext.getContextParam(CloudbreakTest.AUTOSCALE_SECRET));
        }
        clientEntity.cloudbreakClient = singletonCloudbreakClient;
    }

    public static synchronized CloudbreakClient createProxyCloudbreakClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        CloudbreakClient clientEntity = new CloudbreakClient();
        clientEntity.cloudbreakClient = new ProxyCloudbreakClient(
                testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                testParameter.get(CloudbreakTest.CAAS_PROTOCOL),
                testParameter.get(CloudbreakTest.CAAS_ADDRESS),
                cloudbreakUser.getToken(),
                new ConfigKey(false, true, true));
        return clientEntity;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}

