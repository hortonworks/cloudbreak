package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.client.CloudbreakApiKeyClient;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

public class CloudbreakClient extends MicroserviceClient {
    public static final String CLOUDBREAK_CLIENT = "CLOUDBREAK_CLIENT";

    private static com.sequenceiq.cloudbreak.client.CloudbreakClient singletonCloudbreakClient;

    private static CloudbreakInternalCrnClient singletonCloudbreakInternalCrnClient;

    private static String crn;

    private com.sequenceiq.cloudbreak.client.CloudbreakClient cloudbreakClient;

    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

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
        if (singletonCloudbreakClient == null) {
            singletonCloudbreakInternalCrnClient =
                    createCloudbreakInternalCrnClient(integrationTestContext.getContextParam(CloudbreakTest.CLOUDBREAK_SERVER_INTERNAL_ROOT));
        }
        clientEntity.cloudbreakClient = singletonCloudbreakClient;
        clientEntity.cloudbreakInternalCrnClient = singletonCloudbreakInternalCrnClient;
    }

    public static synchronized CloudbreakClient createProxyCloudbreakClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        CloudbreakClient clientEntity = new CloudbreakClient();
        clientEntity.cloudbreakClient = new CloudbreakApiKeyClient(
                testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_ROOT),
                new ConfigKey(false, true, true))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        clientEntity.cloudbreakInternalCrnClient = createCloudbreakInternalCrnClient(testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_INTERNAL_ROOT));
        return clientEntity;
    }

    public static synchronized CloudbreakInternalCrnClient createCloudbreakInternalCrnClient(String serverRoot) {
        CloudbreakServiceUserCrnClient cbUserCrnClient = new CloudbreakUserCrnClientBuilder(serverRoot)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        return new CloudbreakInternalCrnClient(cbUserCrnClient, new InternalCrnBuilder(Crn.Service.IAM));
    }

    public com.sequenceiq.cloudbreak.client.CloudbreakClient getCloudbreakClient() {
        return cloudbreakClient;
    }

    public CloudbreakInternalCrnClient getCloudbreakInternalCrnClient() {
        return cloudbreakInternalCrnClient;
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

