package com.sequenceiq.it.cloudbreak.dto.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.json.JSONException;
import org.json.JSONObject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.mock.MockCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Clue;
import com.sequenceiq.it.cloudbreak.context.Investigable;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.util.AuditUtil;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;
import com.sequenceiq.sdx.api.model.SdxInternalClusterRequest;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@Prototype
public class SdxInternalTestDto extends AbstractSdxTestDto<SdxInternalClusterRequest, SdxClusterDetailResponse, SdxInternalTestDto>
        implements Purgable<SdxClusterResponse, SdxClient>, Investigable, Searchable {

    private static final String SDX_RESOURCE_NAME = "sdxName";

    private static final String DEFAULT_SDX_NAME = "test-sdx" + '-' + UUID.randomUUID().toString().replaceAll("-", "");

    private static final String DEFAULT_CM_PASSWORD = "Admin123";

    private static final String DEFAULT_CM_USER = "admin";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    public SdxInternalTestDto(TestContext testContext) {
        super(new SdxInternalClusterRequest(), testContext);
    }

    @Override
    public SdxInternalTestDto valid() {
        withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withDefaultSDXSettings()
                .withEnvironmentName(getTestContext().get(EnvironmentTestDto.class).getResponse().getName())
                .withClusterShape(getCloudProvider().getInternalClusterShape())
                .withTags(getCloudProvider().getTags())
                .withRuntimeVersion(commonClusterManagerProperties.getRuntimeVersion());
        return getCloudProvider().sdxInternal(this);
    }

    @Override
    public String getName() {
        return super.getName() == null ? DEFAULT_SDX_NAME : super.getName();
    }

    @Override
    public String getCrn() {
        if (getResponse() == null) {
            throw new IllegalStateException("You have tried to assign to SdxInternalTestDto," +
                    " that hasn't been created and therefore has no Response object yet.");
        }
        return getResponse().getCrn();
    }

    public SdxInternalTestDto withDatabase(SdxDatabaseRequest sdxDatabaseRequest) {
        getRequest().setExternalDatabase(sdxDatabaseRequest);
        return this;
    }

    public SdxInternalTestDto withoutDatabase() {
        final SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setCreate(false);
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        getRequest().setExternalDatabase(sdxDatabaseRequest);
        return this;
    }

    public SdxInternalTestDto withCustomDomain(CustomDomainSettingsV4Request customDomain) {
        getRequest().getStackV4Request().setCustomDomain(customDomain);
        return this;
    }

    public SdxInternalTestDto withRuntimeVersion(String runtimeVersion) {
        getRequest().setRuntime(runtimeVersion);
        return this;
    }

    public SdxInternalTestDto withRangerRazEnabled(boolean rangerRazEnabled) {
        getRequest().setEnableRangerRaz(rangerRazEnabled);
        return this;
    }

    public SdxInternalTestDto withCloudStorage(SdxCloudStorageRequest cloudStorage) {
        getRequest().setCloudStorage(cloudStorage);
        return this;
    }

    public SdxInternalTestDto withStackRequest(StackV4Request stackV4Request) {
        getRequest().setStackV4Request(stackV4Request);
        return this;
    }

    public SdxInternalTestDto addTags(Map<String, String> tags) {
        getRequest().getStackV4Request().initAndGetTags().getUserDefined().putAll(tags);
        return this;
    }

    private SdxInternalTestDto withStackRequest(ClusterTestDto cluster, StackTestDto stack) {
        stack.withCluster(cluster);
        return withStackRequest(stack.getRequest());
    }

    public SdxInternalTestDto withStackRequest(RunningParameter clusterKey, RunningParameter stackKey) {
        ClusterTestDto cluster = getTestContext().get(clusterKey.getKey());
        StackTestDto stack = getTestContext().get(stackKey.getKey());

        if (cluster == null) {
            throw new IllegalArgumentException("Cluster is null with given key: " + clusterKey.getKey());
        }
        if (stack == null) {
            throw new IllegalArgumentException("Stack is null with given key: " + stackKey.getKey());
        }
        return withStackRequest(cluster, stack);
    }

    public SdxInternalTestDto withDefaultSDXSettings() {
        return withDefaultSDXSettings(Optional.empty());
    }

    public SdxInternalTestDto withDefaultSDXSettings(Optional<Integer> gatewayPort) {
        StackTestDto stack = getTestContext().given(StackTestDto.class);
        ClusterTestDto cluster = getTestContext().given(ClusterTestDto.class);

        getTestContext()
                .given("master", InstanceGroupTestDto.class).withHostGroup(MASTER).withNodeCount(1)
                .given("idbroker", InstanceGroupTestDto.class).withHostGroup(IDBROKER).withNodeCount(1);
        cluster.withName(cluster.getName())
                .withBlueprintName(commonClusterManagerProperties.getInternalSdxBlueprintName())
                .withValidateBlueprint(Boolean.FALSE);
        stack.withName(stack.getName())
                .withInstanceGroupsEntity(InstanceGroupTestDto.sdxHostGroup(getTestContext()))
                .withInstanceGroups(MASTER.getName(), IDBROKER.getName())
                .withCluster(cluster);
        if (gatewayPort.isPresent()) {
            stack.withGatewayPort(gatewayPort.get());
        }
        SdxInternalTestDto sdxInternalTestDto = withStackRequest(stack.getRequest());
        sdxInternalTestDto.withRuntimeVersion(commonClusterManagerProperties.getRuntimeVersion());
        return sdxInternalTestDto;
    }

    public SdxInternalTestDto withTemplate(String template) {
        ClusterTestDto cluster = getTestContext().given(ClusterTestDto.class);
        cluster.withBlueprintName(String.format(template, commonClusterManagerProperties.getRuntimeVersion()));
        return this;
    }

    public SdxInternalTestDto withClusterTemplateName(String template) {
        ClusterTestDto cluster = getTestContext().given(ClusterTestDto.class);
        cluster.withBlueprintName(template);
        return this;
    }

    public SdxInternalTestDto withTemplate(JSONObject templateJson) {
        StackTestDto stack = getTestContext().given(StackTestDto.class);
        ClusterTestDto cluster = getTestContext().given(ClusterTestDto.class);

        cluster.withName(cluster.getName())
                .withBlueprintName(getBlueprintName())
                .withValidateBlueprint(Boolean.FALSE)
                .withPassword(getClusterPassword(templateJson))
                .withUserName(getClusterUser(templateJson));
        stack.withName(getName())
                .withImageSettings(getImageCatalog(getTestContext().given(ImageSettingsTestDto.class), templateJson))
                .withPlacement(getTestContext().given(PlacementSettingsTestDto.class))
                .withInstanceGroupsEntity(InstanceGroupTestDto.sdxHostGroup(getTestContext()))
                .withInstanceGroups(MASTER.getName(), IDBROKER.getName())
                .withStackAuthentication(getAuthentication(getTestContext().given(StackAuthenticationTestDto.class), templateJson))
//                .withGatewayPort(getGatewayPort(stack, templateJson))
                .withCluster(cluster);
        SdxInternalTestDto sdxInternalTestDto = withStackRequest(stack.getRequest());
        sdxInternalTestDto.withRuntimeVersion(commonClusterManagerProperties.getRuntimeVersion());
        return sdxInternalTestDto;
    }

    private String getBlueprintName() {
        return commonClusterManagerProperties().getInternalSdxBlueprintName();
    }

    public SdxInternalTestDto withTags(Map<String, String> tags) {
        getRequest().addTags(tags);
        return this;
    }

    public SdxInternalTestDto withClusterShape(SdxClusterShape shape) {
        getRequest().setClusterShape(shape);
        return this;
    }

    public SdxInternalTestDto withEnvironment() {
        EnvironmentTestDto environment = getTestContext().given(EnvironmentTestDto.class);
        if (environment == null) {
            throw new IllegalArgumentException(String.format("Environment has not been provided for this internal Sdx: '%s' response!", getName()));
        }
        return withEnvironmentName(environment.getResponse().getName());
    }

    private SdxInternalTestDto withEnvironmentDto(EnvironmentTestDto environmentTestDto) {
        return withEnvironmentName(environmentTestDto.getResponse().getName());
    }

    public SdxInternalTestDto withEnvironmentClass(Class<EnvironmentTestDto> environmentClass) {
        EnvironmentTestDto environment = getTestContext().get(environmentClass.getSimpleName());
        if (environment == null) {
            throw new IllegalArgumentException(String.format("Environment has not been provided for this Sdx: '%s' response!", getName()));
        }
        return withEnvironmentName(environment.getResponse().getName());
    }

    public SdxInternalTestDto withEnvironmentKey(RunningParameter key) {
        EnvironmentTestDto environment = getTestContext().get(key.getKey());
        if (environment == null) {
            throw new IllegalArgumentException(String.format("Environment has not been provided for this internal Sdx: '%s' response!", getName()));
        }
        return withEnvironmentName(environment.getResponse().getName());
    }

    public SdxInternalTestDto withEnvironmentName(String environmentName) {
        getRequest().setEnvironment(environmentName);
        return this;
    }

    public SdxInternalTestDto withName(String name) {
        setName(name);
        return this;
    }

    @Override
    public String getResourceNameType() {
        return SDX_RESOURCE_NAME;
    }

    public SdxInternalTestDto withImageCatalogNameOnly(String imageCatalogName) {
        ImageSettingsV4Request image = new ImageSettingsV4Request();
        image.setCatalog(imageCatalogName);
        getRequest().getStackV4Request().setImage(image);
        return this;
    }

    public SdxInternalTestDto withImageCatalogNameAndImageId(String imageCatalogName, String imageId) {
        ImageSettingsV4Request image = new ImageSettingsV4Request();
        image.setCatalog(imageCatalogName);
        image.setId(imageId);
        getRequest().getStackV4Request().setImage(image);
        return this;
    }

    public SdxInternalTestDto await(SdxClusterStatusResponse status) {
        return getTestContext().await(this, Map.of("status", status), emptyRunningParameter());
    }

    public SdxInternalTestDto await(SdxClusterStatusResponse status, RunningParameter runningParameter) {
        return getTestContext().await(this, Map.of("status", status), runningParameter);
    }

    public SdxInternalTestDto awaitForFlow() {
        return awaitForFlow(emptyRunningParameter());
    }

    public SdxInternalTestDto awaitForFlowFail() {
        return awaitForFlow(emptyRunningParameter().withWaitForFlowFail());
    }

    @Override
    public SdxInternalTestDto awaitForFlow(RunningParameter runningParameter) {
        return getTestContext().awaitForFlow(this, runningParameter);
    }

    private Map<List<String>, InstanceStatus> getInstanceStatusMapIfAvailableInResponse(Supplier<Map<List<String>, InstanceStatus>> instanceStatusMapSupplier) {
        if (checkResponseHasInstanceGroups()) {
            return instanceStatusMapSupplier.get();
        } else {
            LOGGER.info("Response doesn't has instance groups");
            return Collections.emptyMap();
        }
    }

    private boolean checkResponseHasInstanceGroups() {
        return getResponse() != null && getResponse().getStackV4Response() != null && getResponse().getStackV4Response().getInstanceGroups() != null;
    }

    public SdxInternalTestDto awaitForMasterDeletedOnProvider() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                getResponse().getStackV4Response().getInstanceGroups().stream()
                        .filter(instanceGroupV4Response -> MASTER.getName().equals(instanceGroupV4Response.getName()))
                        .collect(Collectors.toMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream()
                                .map(InstanceMetaDataV4Response::getInstanceId).collect(Collectors.toList()),
                        instanceMetaDataV4Response -> InstanceStatus.DELETED_ON_PROVIDER_SIDE)));
        return awaitForInstance(instanceStatusMap);
    }

    public SdxInternalTestDto awaitForHealthyInstances() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                InstanceUtil.getInstanceStatusMap(getResponse().getStackV4Response()));
        return awaitForInstance(instanceStatusMap);
    }

    public SdxInternalTestDto awaitForStoppedInstances() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                getResponse().getStackV4Response().getInstanceGroups().stream().collect(Collectors.toMap(
                        instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream()
                                .map(InstanceMetaDataV4Response::getInstanceId).collect(Collectors.toList()),
                        instanceMetaDataV4Response -> InstanceStatus.STOPPED)));
        return awaitForInstance(instanceStatusMap);
    }

    public SdxInternalTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses) {
        return awaitForInstance(statuses, emptyRunningParameter());
    }

    public SdxInternalTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses, RunningParameter runningParameter) {
        return getTestContext().awaitForInstance(this, statuses, runningParameter);
    }

    @Override
    public CloudbreakTestDto refresh() {
        LOGGER.info("Refresh SDX Internal with name: {}", getName());
        return when(sdxTestClient.refreshInternal(), key("refresh-sdx-" + getName()));
    }

    @Override
    public void cleanUp(TestContext context, MicroserviceClient client) {
        LOGGER.info("Cleaning up sdx internal with name: {}", getName());
        if (getResponse() != null) {
            when(sdxTestClient.forceDeleteInternal(), key("delete-sdx-" + getName()));
            await(DELETED, new RunningParameter().withSkipOnFail(true));
        } else {
            LOGGER.info("Sdx internal: {} response is null!", getName());
        }
    }

    @Override
    public List<SdxClusterResponse> getAll(SdxClient client) {
        SdxEndpoint sdxEndpoint = client.getDefaultClient().sdxEndpoint();
        return sdxEndpoint.list(null, false).stream()
                .filter(s -> s.getName() != null)
                .map(s -> {
                    SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
                    sdxClusterResponse.setName(s.getName());
                    return sdxClusterResponse;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean deletable(SdxClusterResponse entity) {
        return getName().startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    @Override
    public void delete(TestContext testContext, SdxClusterResponse entity, SdxClient client) {
        String sdxName = entity.getName();
        try {
            client.getDefaultClient().sdxEndpoint().delete(getName(), false);
            testContext.await(this, Map.of("status", DELETED), key("wait-purge-sdx-" + getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", sdxName, ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public Class<SdxClient> client() {
        return SdxClient.class;
    }

    private ImageSettingsTestDto getImageCatalog(ImageSettingsTestDto image, JSONObject templateJson) {
        try {
            return image
                    .withImageCatalog(templateJson.getJSONObject("image").getString("catalog"))
                    .withImageId(templateJson.getJSONObject("image").getString("id"));
        } catch (JSONException e) {
            LOGGER.error("Cannot get Image Catalog from template: {}", templateJson, e);
            return getCloudProvider().imageSettings(getTestContext().get(ImageSettingsTestDto.class));
        }
    }

    private StackAuthenticationTestDto getAuthentication(StackAuthenticationTestDto authentication, JSONObject templateJson) {
        try {
            return authentication.withPublicKeyId(templateJson.getJSONObject("authentication").getString("publicKeyId"));
        } catch (JSONException e) {
            LOGGER.error("Cannot get Authentication from template: {}", templateJson, e);
            return getCloudProvider().stackAuthentication(getTestContext().get(StackAuthenticationTestDto.class));
        }
    }

    private Integer getGatewayPort(StackTestDtoBase stack, JSONObject templateJson) {
        try {
            return Integer.parseInt(templateJson.getString("gatewayPort"));
        } catch (JSONException e) {
            LOGGER.error("Cannot get Gateway Port from template: {}", templateJson, e);
            MockCloudProvider mock = new MockCloudProvider();
            return mock.gatewayPort(stack);
        }
    }

    private String getClusterPassword(JSONObject templateJson) {
        try {
            return templateJson.getJSONObject("cluster").getString("password");
        } catch (JSONException e) {
            LOGGER.error("Cannot get Cluster Password from template: {}", templateJson, e);
            String password = commonClusterManagerProperties.getClouderaManager().getDefaultPassword();
            return password == null ? DEFAULT_CM_PASSWORD : password;
        }
    }

    private String getClusterUser(JSONObject templateJson) {
        try {
            return templateJson.getJSONObject("cluster").getString("userName");
        } catch (JSONException e) {
            LOGGER.error("Cannot get Cluster User from template: {}", templateJson, e);
            String user = commonClusterManagerProperties.getClouderaManager().getDefaultUser();
            return user == null ? DEFAULT_CM_USER : user;
        }
    }

    public SdxRepairRequest getSdxRepairRequest(String... hostGroups) {
        SdxRepairTestDto repair = getCloudProvider().sdxRepair(given(SdxRepairTestDto.class).withHostGroupNames(Arrays.asList(hostGroups)));
        if (repair == null) {
            throw new IllegalArgumentException("SDX Repair does not exist!");
        }
        return repair.getRequest();
    }

    public SdxClusterResizeRequest getSdxResizeRequest() {
        SdxResizeTestDto resize = given(SdxResizeTestDto.class);
        if (resize == null) {
            throw new IllegalArgumentException("SDX Resize does not exist!");
        }
        return resize.getRequest();
    }

    @Override
    public Clue investigate() {
        if (getResponse() == null || getResponse().getCrn() == null) {
            return null;
        }
        AuditEventV4Responses auditEvents = AuditUtil.getAuditEvents(
                getTestContext().getMicroserviceClient(CloudbreakClient.class),
                CloudbreakEventService.DATAHUB_RESOURCE_TYPE,
                null,
                getResponse().getCrn());
        boolean hasSpotTermination = (getResponse().getStackV4Response() == null) ? false : getResponse().getStackV4Response().getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetadata().stream())
                .anyMatch(metadata -> InstanceStatus.DELETED_BY_PROVIDER == metadata.getInstanceStatus());
        return new Clue("SDX", auditEvents, getResponse(), hasSpotTermination);
    }

    protected CommonClusterManagerProperties commonClusterManagerProperties() {
        return commonClusterManagerProperties;
    }

    @Override
    public String getSearchId() {
        return getName();
    }
}
