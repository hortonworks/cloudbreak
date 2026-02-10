package com.sequenceiq.it.cloudbreak.microservice;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.ApiKeyRequestFilter;
import com.sequenceiq.cloudbreak.client.CloudbreakApiKeyClient;
import com.sequenceiq.cloudbreak.client.CloudbreakApiKeyEndpoints;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
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
import com.sequenceiq.it.cloudbreak.dto.restart.RestartInstancesTestDto;
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

    private static final long WORKSPACE_ID = 0L;

    private com.sequenceiq.cloudbreak.client.CloudbreakClient cloudbreakClient;

    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    private WebTarget rawClient;

    private com.sequenceiq.cloudbreak.client.CloudbreakClient alternativeCloudbreakClient;

    private CloudbreakInternalCrnClient alternativeCloudbreakInternalCrnClient;

    private WebTarget alternativeRawClient;

    private Long workspaceId;

    public CloudbreakClient(CloudbreakUser cloudbreakUser, RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator, String serviceAddress,
            String internalAddress, String alternativeServiceAddress, String alternativeInternalAddress, String alternativeAddress) {
        setActing(cloudbreakUser);
        ConfigKey configKey = new ConfigKey(false, true, true);

        cloudbreakClient = createCloudbreakClient(cloudbreakUser, serviceAddress, configKey);
        cloudbreakInternalCrnClient = createCloudbreakInternalCrnClient(internalAddress, regionAwareInternalCrnGenerator);
        rawClient = createRawWebTarget(configKey, serviceAddress, CoreApi.API_ROOT_CONTEXT,
                cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());

        if (isNotEmpty(alternativeServiceAddress) && isNotEmpty(alternativeInternalAddress)) {
            alternativeCloudbreakClient = createCloudbreakClient(cloudbreakUser, alternativeServiceAddress, configKey);
            alternativeCloudbreakInternalCrnClient = createCloudbreakInternalCrnClient(alternativeInternalAddress, regionAwareInternalCrnGenerator);
            alternativeRawClient = createRawWebTarget(configKey, alternativeAddress, CoreApi.API_ROOT_CONTEXT,
                    cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        }
    }

    private CloudbreakApiKeyEndpoints createCloudbreakClient(CloudbreakUser cloudbreakUser, String serviceAddress, ConfigKey configKey) {
        return new CloudbreakApiKeyClient(serviceAddress, configKey)
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeCloudbreakClient.flowPublicEndpoint();
        } else {
            return cloudbreakClient.flowPublicEndpoint();
        }
    }

    @Override
    public CloudbreakWaitObject waitObject(CloudbreakTestDto entity, String name, Map<String, Status> desiredStatuses,
            TestContext testContext, Set<Status> ignoredFailedStatuses) {
        return new CloudbreakWaitObject(this, name, desiredStatuses, testContext.getActingUserCrn().getAccountId(), ignoredFailedStatuses, testContext);
    }

    @Override
    public com.sequenceiq.cloudbreak.client.CloudbreakClient getDefaultClient(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeCloudbreakClient;
        } else {
            return cloudbreakClient;
        }
    }

    private WebTarget createRawWebTarget(ConfigKey configKey, String serviceAddress, String apiRoot,
            String accessKey, String secretKey) {
        Client client = RestClientUtil.get(configKey);
        WebTarget webTarget = client.target(serviceAddress).path(apiRoot);
        webTarget.register(new ApiKeyRequestFilter(accessKey, secretKey));
        return webTarget;
    }

    private CloudbreakInternalCrnClient createCloudbreakInternalCrnClient(String serverRoot,
            RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        CloudbreakServiceUserCrnClient cbUserCrnClient = new CloudbreakUserCrnClientBuilder(serverRoot)
                .withCertificateValidation(false)
                .withIgnorePreValidation(true)
                .withDebug(true)
                .build();
        return new CloudbreakInternalCrnClient(cbUserCrnClient, regionAwareInternalCrnGenerator);
    }

    public Long getWorkspaceId() {
        return WORKSPACE_ID;
    }

    public WebTarget getRawClient(TestContext testContext) {
        if (testContext.shouldUseAlternativeEndpoints()) {
            return alternativeRawClient;
        } else {
            return rawClient;
        }
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
                DistroXChangeImageCatalogTestDto.class.getSimpleName(),
                RestartInstancesTestDto.class.getSimpleName());
    }

    @Override
    public CloudbreakServiceCrnEndpoints getInternalClient(TestContext testContext) {
        checkIfInternalClientAllowed(testContext);
        CloudbreakInternalCrnClient client;
        if (testContext.shouldUseAlternativeEndpoints()) {
            client = alternativeCloudbreakInternalCrnClient;
        } else {
            client = cloudbreakInternalCrnClient;
        }
        return client.withInternalCrn();
    }

    @Override
    public CloudbreakServiceCrnEndpoints getInternalClientWithoutChecks(TestContext testContext) {
        CloudbreakInternalCrnClient client;
        if (testContext.shouldUseAlternativeEndpoints()) {
            client = alternativeCloudbreakInternalCrnClient;
        } else {
            client = cloudbreakInternalCrnClient;
        }
        return client.withInternalCrn();
    }

    @Override
    public <O extends Enum<O>> InstanceWaitObject waitInstancesObject(CloudbreakTestDto entity, TestContext testContext,
            List<String> instanceIds, O instanceStatus) {
        return new CloudbreakInstanceWaitObject(testContext, entity.getName(), instanceIds, (InstanceStatus) instanceStatus);
    }
}

