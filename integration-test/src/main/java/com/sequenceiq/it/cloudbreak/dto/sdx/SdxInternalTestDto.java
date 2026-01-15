package com.sequenceiq.it.cloudbreak.dto.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.util.Strings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SecurityV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.assertion.util.InstanceIPCollectorUtil;
import com.sequenceiq.it.cloudbreak.await.Await;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
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
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.util.AuditUtil;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.StructuredEventUtil;
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
import com.sequenceiq.sdx.api.model.SdxRecipe;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;

@Prototype
public class SdxInternalTestDto extends AbstractSdxTestDto<SdxInternalClusterRequest, SdxClusterDetailResponse, SdxInternalTestDto>
        implements Purgable<SdxClusterResponse, SdxClient>, Investigable, Searchable {

    public static final String SDX_RESOURCE_NAME = "sdxName";

    private static final String DEFAULT_SDX_NAME = "test-sdx" + '-' + UUID.randomUUID().toString().replaceAll("-", "");

    private static final String DEFAULT_CM_PASSWORD = "Admin123";

    private static final String DEFAULT_CM_USER = "admin";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    private final Map<HostGroupType, String> privateIps = new EnumMap<>(HostGroupType.class);

    public SdxInternalTestDto(TestContext testContext) {
        super(new SdxInternalClusterRequest(), testContext);
    }

    @Override
    public SdxInternalTestDto valid() {
        withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withDefaultSDXSettings()
                .withClusterShape(getCloudProvider().getInternalClusterShape())
                .withTags(getCloudProvider().getTags())
                .withEnableMultiAz(getCloudProvider().isMultiAZ())
                .withImageValidationCatalogAndImageIfPresent()
                .withEmbeddedDatabase();
        EnvironmentTestDto environmentTestDto = getTestContext().get(EnvironmentTestDto.class);

        if (environmentTestDto != null && environmentTestDto.getResponse() != null) {
            withEnvironmentName(environmentTestDto.getName());
        }
        return getCloudProvider().sdxInternal(this);
    }

    @Override
    public String getName() {
        return super.getName() == null ? DEFAULT_SDX_NAME : super.getName();
    }

    @Override
    public String getCrn() {
        if (getResponse() == null) {
            return null;
        }
        return getResponse().getCrn();
    }

    public SdxInternalTestDto withAutoTls() {
        getRequest().getStackV4Request().getCluster().getCm().setEnableAutoTls(true);
        return this;
    }

    public SdxInternalTestDto withDatabase(SdxDatabaseRequest sdxDatabaseRequest) {
        getRequest().setExternalDatabase(sdxDatabaseRequest);
        return this;
    }

    public SdxInternalTestDto withEmbeddedDatabase() {
        if (getRequest().getExternalDatabase() == null) {
            SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
            sdxDatabaseRequest.setCreate(false);
            sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
            getRequest().setExternalDatabase(sdxDatabaseRequest);
        }
        return this;
    }

    public SdxInternalTestDto withInstanceType(String instanceType) {
        getRequest().getStackV4Request().getInstanceGroups().forEach(ig -> ig.getTemplate().setInstanceType(instanceType));
        return this;
    }

    public SdxInternalTestDto withCustomDomain(CustomDomainSettingsV4Request customDomain) {
        getRequest().getStackV4Request().setCustomDomain(customDomain);
        return this;
    }

    public SdxInternalTestDto withVariant(String variant) {
        getRequest().getStackV4Request().setVariant(variant);
        return this;
    }

    public SdxInternalTestDto withRuntimeVersion(String runtimeVersion) {
        getRequest().setRuntime(runtimeVersion);
        getRequest().getStackV4Request().getCluster()
                .setBlueprintName(commonClusterManagerProperties.getInternalSdxBlueprintNameWithRuntimeVersion(runtimeVersion));
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

    public SdxInternalTestDto withCloudStorage() {
        SdxCloudStorageTestDto cloudStorage = getCloudProvider().cloudStorage(given(SdxCloudStorageTestDto.class));
        if (cloudStorage == null) {
            throw new IllegalArgumentException("SDX Cloud Storage does not exist!");
        }
        return withCloudStorage(cloudStorage.getRequest());
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

    public InstanceGroupV4Response findInstanceGroupByName(String name) {
        return getResponse().getStackV4Response().getInstanceGroups().stream().filter(ig -> name.equals(ig.getName())).findFirst()
                .orElseThrow(() -> new TestFailException("Unable to find Data Lake instance group based on the following name: " + name));
    }

    public SdxInternalTestDto withDefaultSDXSettings() {
        return withDefaultSDXSettings(Optional.empty());
    }

    public SdxInternalTestDto withDefaultSDXSettings(Optional<Integer> gatewayPort) {
        StackTestDto stack = getTestContext().given(StackTestDto.class);
        boolean clusterIsGiven = getTestContext().get(ClusterTestDto.class.getSimpleName()) == null;
        ClusterTestDto cluster = getTestContext().given(ClusterTestDto.class);
        if (clusterIsGiven) {
            cluster = getTestContext().given(ClusterTestDto.class);
            cluster.withName(cluster.getName())
                    .withBlueprintName(commonClusterManagerProperties.getInternalSdxBlueprintName())
                    .withValidateBlueprint(Boolean.FALSE);
        }

        getTestContext()
                .given("master", InstanceGroupTestDto.class).withHostGroup(MASTER).withNodeCount(1)
                .given("idbroker", InstanceGroupTestDto.class).withHostGroup(IDBROKER).withNodeCount(1);
        stack.withName(stack.getName())
                .withInstanceGroupsEntity(InstanceGroupTestDto.sdxHostGroup(getTestContext()))
                .withInstanceGroups(MASTER.getName(), IDBROKER.getName())
                .withCluster(cluster);
        if (gatewayPort.isPresent()) {
            stack.withGatewayPort(gatewayPort.get());
        }
        return withStackRequest(stack.getRequest());
    }

    public SdxInternalTestDto withTelemetry(String telemetry) {
        TelemetryTestDto telemetryTestDto = getTestContext().get(telemetry);
        if (telemetryTestDto != null) {
            getRequest().getStackV4Request().setTelemetry(telemetryTestDto.getRequest());
        }
        return this;
    }

    public SdxInternalTestDto withTemplate(String template) {
        ClusterTestDto cluster = getTestContext().given(ClusterTestDto.class);
        cluster.withBlueprintName(String.format(template, commonClusterManagerProperties.getRuntimeVersion()));
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
                .withStackAuthentication(getAuthenticationFromTemplate(getTestContext().given(StackAuthenticationTestDto.class), templateJson))
                .withCluster(cluster);
        return withStackRequest(stack.getRequest());
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
        EnvironmentTestDto environment = getTestContext().get(EnvironmentTestDto.class);
        if (environment == null) {
            throw new IllegalArgumentException(String.format("Environment has not been provided for this internal Sdx: '%s' response!", getName()));
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

    public SdxInternalTestDto withImageCatalogNameAndImageId(String imageCatalogName, String imageId) {
        if (!Strings.isNullOrEmpty(imageCatalogName) && !Strings.isNullOrEmpty(imageId)) {
            ImageSettingsV4Request image = new ImageSettingsV4Request();
            image.setCatalog(imageCatalogName);
            image.setId(imageId);
            getRequest().getStackV4Request().setImage(image);
        } else {
            LOGGER.warn("Catalog [{}] or image [{}] is null or empty", imageCatalogName, imageId);
        }
        return this;
    }

    public SdxInternalTestDto withImageValidationCatalogAndImageIfPresent() {
        return withImageCatalogNameAndImageId(commonCloudProperties.getImageValidation().getSourceCatalogName(),
                commonCloudProperties.getImageValidation().getImageUuid());
    }

    public SdxInternalTestDto withDefaultImage() {
        getRequest().getStackV4Request().setImage(null);
        return this;
    }

    public SdxInternalTestDto withEnableMultiAz(boolean enableMultiAz) {
        getRequest().setEnableMultiAz(enableMultiAz);
        return this;
    }

    public SdxInternalTestDto withEnableMultiAz() {
        getRequest().setEnableMultiAz(getCloudProvider().isMultiAZ());
        return this;
    }

    public SdxInternalTestDto withRecipe(String name, String hostGroup) {
        SdxRecipe sdxRecipe = new SdxRecipe();
        sdxRecipe.setName(name);
        sdxRecipe.setHostGroup(hostGroup);
        if (null == getRequest().getRecipes()) {
            getRequest().setRecipes(Set.of(sdxRecipe));
        } else {
            getRequest().getRecipes().add(sdxRecipe);
        }
        return this;
    }

    public SdxInternalTestDto await(SdxClusterStatusResponse status) {
        return getTestContext().await(this, Map.of("status", status), emptyRunningParameter());
    }

    public SdxInternalTestDto await(SdxClusterStatusResponse status, RunningParameter runningParameter) {
        return getTestContext().await(this, Map.of("status", status), runningParameter);
    }

    public SdxInternalTestDto await(SdxClusterStatusResponse status, RunningParameter runningParameter, Await<SdxInternalTestDto, SdxClient> customAction) {
        return getTestContext().await(this, Map.of("status", status), runningParameter, customAction);
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
            LOGGER.info("Response doesn't have instance groups");
            return Collections.emptyMap();
        }
    }

    private boolean checkResponseHasInstanceGroups() {
        return getResponse() != null && getResponse().getStackV4Response() != null && getResponse().getStackV4Response().getInstanceGroups() != null;
    }

    public SdxInternalTestDto awaitForMasterDeletedOnProvider() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                getResponse().getStackV4Response().getInstanceGroups().stream()
                        .filter(instanceGroup -> MASTER.getName().equals(instanceGroup.getName()))
                        .collect(Collectors.toMap(instanceGroup -> instanceGroup.getMetadata().stream()
                                        .map(InstanceMetaDataV4Response::getInstanceId).collect(Collectors.toList()),
                                instanceMetaData -> InstanceStatus.DELETED_ON_PROVIDER_SIDE)));
        return awaitForInstance(instanceStatusMap);
    }

    public SdxInternalTestDto awaitForHealthyInstances() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                InstanceUtil.getInstanceStatusMapForStatus(getResponse().getStackV4Response(), InstanceStatus.SERVICES_HEALTHY));
        return awaitForInstance(instanceStatusMap);
    }

    public SdxInternalTestDto awaitForDeletedInstancesOnProvider() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                getResponse().getStackV4Response().getInstanceGroups().stream().collect(Collectors.toMap(
                        instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream()
                                .map(InstanceMetaDataV4Response::getInstanceId).collect(Collectors.toList()),
                        instanceMetaDataV4Response -> InstanceStatus.DELETED_ON_PROVIDER_SIDE)));
        return awaitForInstance(instanceStatusMap);
    }

    public SdxInternalTestDto awaitForStoppedInstances() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                InstanceUtil.getInstanceStatusMapForStatus(getResponse().getStackV4Response(), InstanceStatus.STOPPED));
        return awaitForInstance(instanceStatusMap);
    }

    public SdxInternalTestDto awaitForStartingInstances() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                InstanceUtil.getInstanceStatusMapForStatus(getResponse().getStackV4Response(), InstanceStatus.SERVICES_RUNNING));
        return awaitForInstance(instanceStatusMap);
    }

    public SdxInternalTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses) {
        return awaitForInstance(statuses, emptyRunningParameter());
    }

    public SdxInternalTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses, RunningParameter runningParameter) {
        return getTestContext().awaitForInstance(this, statuses, runningParameter);
    }

    public SdxInternalTestDto awaitForInstancesToExist() {
        return awaitForInstancesToExist(emptyRunningParameter());
    }

    public SdxInternalTestDto awaitForInstancesToExist(RunningParameter runningParameter) {
        return getTestContext().awaitForInstancesToExist(this, runningParameter);
    }

    @Override
    public CloudbreakTestDto refresh() {
        LOGGER.info("Refresh SDX Internal with name: {}", getName());
        return when(sdxTestClient.refreshInternal(), key("refresh-sdx-" + getName()).withSkipOnFail(false).withLogError(false));
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

    private StackAuthenticationTestDto getAuthenticationFromTemplate(StackAuthenticationTestDto authentication, JSONObject templateJson) {
        // for testSDXFromTemplateCanBeCreatedThenDeletedSuccessfully mock test.
        String templatePublicKeyId;
        StackAuthenticationV4Request request = authentication.getRequest();
        List<String> authParameterKeys = List.of("publicKeyId", "publicKey", "loginUserName");
        List<String> authParametersValues = new ArrayList<>();
        Map<String, String> fetchedTemplateAuthentication;

        try {
            JSONObject templateAuthentication = templateJson.getJSONObject("authentication");
            fetchedTemplateAuthentication = JsonUtil.readValue(templateAuthentication.toString(),
                    new TypeReference<>() {
                    });
        } catch (JSONException | IOException e) {
            LOGGER.warn("Cannot get or fetch Authentication from SDX template (going further with Cloud Provider defaults): {}", templateJson, e);
            return getCloudProvider().stackAuthentication(authentication);
        }

        LOGGER.info("Fetched Authentication parameters: {}", fetchedTemplateAuthentication);
        authParameterKeys.forEach(parameter -> authParametersValues.add(fetchedTemplateAuthentication.entrySet().stream()
                .filter(authParam -> parameter.equalsIgnoreCase(authParam.getKey()))
                .map(Entry::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null)));
        LOGGER.info("Found Authentication parameters: {}", authParametersValues);
        authentication.withPublicKeyId(StringUtils.isBlank(authParametersValues.get(0))
                ? request.getPublicKeyId()
                : authParametersValues.get(0));
        authentication.withPublicKey(StringUtils.isBlank(authParametersValues.get(1))
                ? request.getPublicKey()
                : authParametersValues.get(1));
        authentication.withLoginUserName(StringUtils.isBlank(authParametersValues.get(2))
                ? request.getLoginUserName()
                : authParametersValues.get(2));
        return authentication;
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

    public SdxUpgradeRequest getSdxUpgradeRequest() {
        SdxUpgradeTestDto upgrade = given(SdxUpgradeTestDto.class);
        if (upgrade == null) {
            throw new IllegalArgumentException("SDX Upgrade does not exist!");
        }
        return upgrade.getRequest();
    }

    public SdxClusterResizeRequest getSdxResizeRequest() {
        SdxResizeTestDto resize = given(SdxResizeTestDto.class);
        if (resize == null) {
            throw new IllegalArgumentException("SDX Resize does not exist!");
        }
        return resize.getRequest();
    }

    public SdxRecoveryRequest getSdxRecoveryRequest() {
        SdxRecoveryTestDto recovery = given(SdxRecoveryTestDto.class);
        if (recovery == null) {
            throw new IllegalArgumentException("SDX Recovery does not exist!");
        }
        return recovery.getRequest();
    }

    @Override
    public Clue investigate() {
        if (getResponse() == null || getResponse().getCrn() == null) {
            return null;
        }
        String resourceName = getResponse().getName();
        String resourceCrn = getResponse().getCrn();
        StackV4Response stackResponse = getResponse().getStackV4Response();
        setCloudPlatformFromStack(stackResponse);
        AuditEventV4Responses auditEvents = AuditUtil.getAuditEvents(
                getTestContext().getMicroserviceClient(CloudbreakClient.class),
                CloudbreakEventService.DATALAKE_RESOURCE_TYPE,
                null,
                resourceCrn);
        List<CDPStructuredEvent> structuredEvents = List.of();
        if (getResponse() != null && resourceCrn != null) {
            CDPStructuredEventV1Endpoint cdpStructuredEventV1Endpoint =
                    getTestContext().getMicroserviceClient(SdxClient.class).getDefaultClient().structuredEventsV1Endpoint();
            structuredEvents = StructuredEventUtil.getAuditEvents(cdpStructuredEventV1Endpoint, resourceCrn);
        }
        List<Searchable> listOfSearchables = List.of(this);
        return new Clue(
                resourceName,
                resourceCrn,
                getCloudStorageUrl(resourceName, resourceCrn),
                getLogSearchUrl(listOfSearchables),
                auditEvents,
                structuredEvents,
                getResponse(),
                hasSpotTermination(stackResponse));
    }

    protected CommonClusterManagerProperties commonClusterManagerProperties() {
        return commonClusterManagerProperties;
    }

    @Override
    public String getSearchId() {
        return getName();
    }

    @Override
    public void deleteForCleanup() {
        try {
            setFlow("SDX Internal deletion", getClientForCleanup().getDefaultClient().sdxEndpoint().deleteByCrn(getCrn(), true));
            awaitForFlow();
        } catch (NotFoundException nfe) {
            LOGGER.info("SDX Internal resource not found, thus cleanup not needed.");
        }
    }

    public SdxInternalTestDto withJavaVersion(Integer javaVersion) {
        getRequest().setJavaVersion(javaVersion);
        return this;
    }

    public SdxInternalTestDto awaitForHostGroups(List<String> hostGroups, InstanceStatus instanceStatus) {
        if (!getTestContext().getExceptionMap().isEmpty()) {
            Log.await(LOGGER, String.format("Await for instances is skipped because of previous error. Required status %s, host group(s) %s.",
                    instanceStatus, hostGroups));
            return this;
        }
        List<InstanceGroupV4Response> instanceGroups = getResponse().getStackV4Response().getInstanceGroups().stream()
                .filter(instanceGroup -> hostGroups.contains(instanceGroup.getName()))
                .toList();
        if (hostGroups.size() == instanceGroups.size()) {
            List<String> instanceIds =
                    instanceGroups.stream().flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                            .map(InstanceMetaDataV4Response::getInstanceId).toList();
            return awaitForInstance(Map.of(instanceIds, instanceStatus));
        } else {
            throw new IllegalStateException("Can't find instance groups with this name: " + hostGroups);
        }
    }

    @Override
    public void setPrivateIpsForLogCollection(TestContext testContext) {
        refreshResponse(testContext, testContext.getSdxClient());
        for (HostGroupType hostGroupType : Set.of(MASTER, IDBROKER)) {
            String hostGroupName = hostGroupType.getName();
            Optional<InstanceMetaDataV4Response> instanceMetaData = getInstanceMetaData(hostGroupName).stream().findFirst();
            if (instanceMetaData.isPresent()) {
                String privateIp = instanceMetaData.get().getPrivateIp();
                if (StringUtils.isNotBlank(privateIp)) {
                    LOGGER.info("Found {} private IP for {} host group!", privateIp, hostGroupName);
                    privateIps.put(hostGroupType, privateIp);
                } else {
                    LOGGER.info("No private IP for {} host group, current instance status is: {}", hostGroupName, instanceMetaData.get().getInstanceStatus());
                }
            }
        }
    }

    @Override
    public Map<HostGroupType, String> getPrivateIpsForLogCollection() {
        return privateIps;
    }

    private void refreshResponse(TestContext testContext, SdxClient client) {
        setResponse(client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(getName(), new HashSet<>())
        );
    }

    private Set<InstanceMetaDataV4Response> getInstanceMetaData(String hostGroupName) {
        Optional<InstanceGroupV4Response> instanceGroupV4Response = Optional.ofNullable(getResponse().getStackV4Response())
                .map(StackV4Response::getInstanceGroups)
                .stream()
                .flatMap(Collection::stream)
                .filter(ig -> ig.getName().equals(hostGroupName))
                .findFirst();
        if (instanceGroupV4Response.isEmpty()) {
            LOGGER.error("There is no valid '{}' group for SDX based on the StackV4Response, check test logs for possible failures.", hostGroupName);
            return Set.of();
        } else if (CollectionUtils.isEmpty(instanceGroupV4Response.get().getMetadata())) {
            LOGGER.error("There is no valid metadata for '{}' group of SDX based on the StackV4Response, check test logs for possible failures.",
                    hostGroupName);
            return Set.of();
        } else {
            return instanceGroupV4Response.get().getMetadata();
        }
    }

    public SdxInternalTestDto withSeLinuxSecurity(String seLinux) {
        SecurityV4Request securityRequest = new SecurityV4Request();
        securityRequest.setSeLinux(seLinux);
        getRequest().setSecurity(securityRequest);
        return this;
    }

    public SeLinux getSelinuxMode() {
        return Optional.ofNullable(getResponse())
                .map(SdxClusterDetailResponse::getSeLinuxPolicy)
                .map(s -> SeLinux.fromStringWithFallback(s))
                .orElse(SeLinux.PERMISSIVE);
    }

    public String getVariant() {
        return getResponse().getStackV4Response().getVariant();
    }

    public List<String> getAllInstanceIps() {
        refresh();
        return InstanceIPCollectorUtil.getAllInstanceIps(this, false);
    }
}
