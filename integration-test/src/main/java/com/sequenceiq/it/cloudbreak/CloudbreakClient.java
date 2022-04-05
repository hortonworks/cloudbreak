package com.sequenceiq.it.cloudbreak;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.ApiKeyRequestFilter;
import com.sequenceiq.cloudbreak.client.CloudbreakApiKeyClient;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.RawCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.audit.AuditTestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.customconfigs.CustomConfigurationsTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.securityrule.SecurityRulesTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.CheckResourceRightTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.CheckRightTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.CloudStorageMatrixTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.DeploymentPreferencesTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.NotificationTestingTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.RenewDistroXCertificateTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.UsedImagesTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.VersionCheckTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak.CloudbreakWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.cloudbreak.CloudbreakInstanceWaitObject;

public class CloudbreakClient extends MicroserviceClient<com.sequenceiq.cloudbreak.client.CloudbreakClient, CloudbreakServiceCrnEndpoints, Status,
        CloudbreakWaitObject> {
    public static final String CLOUDBREAK_CLIENT = "CLOUDBREAK_CLIENT";

    private static com.sequenceiq.cloudbreak.client.CloudbreakClient singletonCloudbreakClient;

    private static CloudbreakInternalCrnClient singletonCloudbreakInternalCrnClient;

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

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        return cloudbreakClient.flowPublicEndpoint();
    }

    @Override
    public CloudbreakWaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, Status> desiredStatuses,
            TestContext testContext, Set<Status> ignoredFailedStatuses) {
        return new CloudbreakWaitObject(this, name, desiredStatuses, testContext.getActingUserCrn().getAccountId(), ignoredFailedStatuses);
    }

    @Override
    public com.sequenceiq.cloudbreak.client.CloudbreakClient getDefaultClient() {
        return cloudbreakClient;
    }

    public static synchronized com.sequenceiq.cloudbreak.client.CloudbreakClient getSingletonCloudbreakClient() {
        return singletonCloudbreakClient;
    }

    public static Function<IntegrationTestContext, CloudbreakClient> getTestContextCloudbreakClient(String key) {
        return testContext -> testContext.getContextParam(key, CloudbreakClient.class);
    }

    public static CloudbreakClient created(RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        CloudbreakClient client = new CloudbreakClient();
        client.setCreationStrategy((c, e) -> CloudbreakClient.createProxyCloudbreakClient(c, e, regionAwareInternalCrnGenerator));
        return client;
    }

    private static synchronized void createProxyCloudbreakClient(IntegrationTestContext integrationTestContext,
        Entity entity, RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
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
            singletonCloudbreakInternalCrnClient = createCloudbreakInternalCrnClient(serviceAddress, regionAwareInternalCrnGenerator);
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

    public static synchronized CloudbreakClient createProxyCloudbreakClient(TestParameter testParameter,
        CloudbreakUser cloudbreakUser, RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        CloudbreakClient clientEntity = new CloudbreakClient();
        clientEntity.setActing(cloudbreakUser);
        ConfigKey configKey = new ConfigKey(false, true, true);
        String serviceAddress = testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_ROOT);
        clientEntity.cloudbreakClient = new CloudbreakApiKeyClient(serviceAddress, configKey)
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        clientEntity.cloudbreakInternalCrnClient = createCloudbreakInternalCrnClient(
                testParameter.get(CloudbreakTest.CLOUDBREAK_SERVER_INTERNAL_ROOT),
                regionAwareInternalCrnGenerator);
        clientEntity.rawClient = createRawWebTarget(configKey, serviceAddress, CoreApi.API_ROOT_CONTEXT,
                cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        return clientEntity;
    }

    public static synchronized CloudbreakInternalCrnClient createCloudbreakInternalCrnClient(String serverRoot,
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        CloudbreakServiceUserCrnClient cbUserCrnClient = new CloudbreakUserCrnClientBuilder(serverRoot)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        return new CloudbreakInternalCrnClient(cbUserCrnClient, regionAwareInternalCrnGenerator);
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

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(DistroXTestDto.class.getSimpleName(),
                BlueprintTestDto.class.getSimpleName(),
                ClusterTemplateTestDto.class.getSimpleName(),
                StackTestDto.class.getSimpleName(),
                VersionCheckTestDto.class.getSimpleName(),
                RecipeTestDto.class.getSimpleName(),
                CustomConfigurationsTestDto.class.getSimpleName(),
                StackMatrixTestDto.class.getSimpleName(),
                NotificationTestingTestDto.class.getSimpleName(),
                RawCloudbreakTestDto.class.getSimpleName(),
                CloudStorageMatrixTestDto.class.getSimpleName(),
                DeploymentPreferencesTestDto.class.getSimpleName(),
                SecurityRulesTestDto.class.getSimpleName(),
                AuditTestDto.class.getSimpleName(),
                CheckRightTestDto.class.getSimpleName(),
                CheckResourceRightTestDto.class.getSimpleName(),
                RenewDistroXCertificateTestDto.class.getSimpleName(),
                ImageCatalogTestDto.class.getSimpleName(),
                UsedImagesTestDto.class.getSimpleName(),
                DistroXChangeImageCatalogTestDto.class.getSimpleName());
    }

    @Override
    public CloudbreakServiceCrnEndpoints getInternalClient(TestContext testContext) {
        checkIfInternalClientAllowed(testContext);
        return cloudbreakInternalCrnClient.withInternalCrn();
    }

    @Override
    public <O extends Enum<O>> InstanceWaitObject waitInstancesObject(CloudbreakTestDto entity, TestContext testContext,
            List<String> instanceIds, O instanceStatus) {
        return new CloudbreakInstanceWaitObject(testContext, entity.getName(), instanceIds, (InstanceStatus) instanceStatus);
    }
}

