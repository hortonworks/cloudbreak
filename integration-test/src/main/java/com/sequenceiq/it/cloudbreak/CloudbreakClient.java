package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.auth.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.client.ApiKeyRequestFilter;
import com.sequenceiq.cloudbreak.client.CloudbreakApiKeyClient;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
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

    private WebTarget rawClient;

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
        ConfigKey configKey = new ConfigKey(false, true, true, TIMEOUT);
        String accessKey = integrationTestContext.getContextParam(CloudbreakTest.ACCESS_KEY);
        String secretKey = integrationTestContext.getContextParam(CloudbreakTest.SECRET_KEY);
        String serviceAddress = null;
        if (singletonCloudbreakClient == null) {
            serviceAddress = integrationTestContext.getContextParam(CloudbreakTest.CLOUDBREAK_SERVER_ROOT);
            singletonCloudbreakClient = new CloudbreakApiKeyClient(serviceAddress, configKey).withKeys(accessKey, secretKey);
        }
        if (singletonCloudbreakClient == null) {
            serviceAddress = integrationTestContext.getContextParam(CloudbreakTest.CLOUDBREAK_SERVER_INTERNAL_ROOT);
            singletonCloudbreakInternalCrnClient = createCloudbreakInternalCrnClient(serviceAddress);
        }
        clientEntity.cloudbreakClient = singletonCloudbreakClient;
        clientEntity.cloudbreakInternalCrnClient = singletonCloudbreakInternalCrnClient;
        clientEntity.rawClient = createRawWebTarget(configKey, serviceAddress, CoreApi.API_ROOT_CONTEXT, accessKey, secretKey);
    }

    private static synchronized WebTarget createRawWebTarget(ConfigKey configKey, String serviceAddress, String apiRoot,
            String accessKey, String secretKey) {
        Client client = RestClientUtil.get(configKey);
        WebTarget webTarget = client.target(serviceAddress).path(apiRoot);
        webTarget.register(new ApiKeyRequestFilter(accessKey, secretKey));
        return webTarget;
    }

    public static synchronized CloudbreakClient createProxyCloudbreakClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        CloudbreakClient clientEntity = new CloudbreakClient();
        clientEntity.setActing(cloudbreakUser);
        ConfigKey configKey = new ConfigKey(false, true, true);
        String serviceAddress = testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_ROOT);
        clientEntity.cloudbreakClient = new CloudbreakApiKeyClient(serviceAddress, configKey)
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        clientEntity.cloudbreakInternalCrnClient = createCloudbreakInternalCrnClient(testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_INTERNAL_ROOT));
        clientEntity.rawClient = createRawWebTarget(configKey, serviceAddress, CoreApi.API_ROOT_CONTEXT,
                cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
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

    public WebTarget getRawClient() {
        return rawClient;
    }
}

