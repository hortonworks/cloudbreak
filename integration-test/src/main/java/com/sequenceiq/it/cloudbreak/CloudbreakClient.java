package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient.CloudbreakEndpoint;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

public class CloudbreakClient extends Entity {
    public static final String CLOUDBREAK_CLIENT = "CLOUDBREAK_CLIENT";

    private static CloudbreakEndpoint singletonCloudbreakClient;

    private static String crn;

    private CloudbreakEndpoint cloudbreakClient;

    private Long workspaceId;

    CloudbreakClient(String newId) {
        super(newId);
    }

    CloudbreakClient() {
        this(CLOUDBREAK_CLIENT);
    }

    public static synchronized CloudbreakEndpoint getSingletonCloudbreakClient() {
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
                    new ConfigKey(false, true, true)).withCrn(integrationTestContext.getContextParam(CloudbreakTest.USER_CRN));
        }
        clientEntity.cloudbreakClient = singletonCloudbreakClient;
    }

    public static synchronized CloudbreakClient createProxyCloudbreakClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        CloudbreakClient clientEntity = new CloudbreakClient();
        clientEntity.cloudbreakClient = new ProxyCloudbreakClient(
                testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                new ConfigKey(false, true, true)).withCrn(cloudbreakUser.getToken());
        return clientEntity;
    }

    public CloudbreakEndpoint getCloudbreakClient() {
        return cloudbreakClient;
    }

    public void setCloudbreakClient(CloudbreakEndpoint cloudbreakClient) {
        this.cloudbreakClient = cloudbreakClient;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}

