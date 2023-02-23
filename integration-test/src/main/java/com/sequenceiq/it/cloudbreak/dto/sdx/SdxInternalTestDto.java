package com.sequenceiq.it.cloudbreak.dto.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
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
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;

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
                .withClusterShape(getCloudProvider().getInternalClusterShape())
                .withTags(getCloudProvider().getTags())
                .withRuntimeVersion(commonClusterManagerProperties.getRuntimeVersion())
                .withEnableMultiAz(getCloudProvider().isMultiAZ());
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

    public SdxInternalTestDto withDatabase(SdxDatabaseRequest sdxDatabaseRequest) {
        getRequest().setExternalDatabase(sdxDatabaseRequest);
        return this;
    }

    public SdxInternalTestDto withoutDatabase() {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setCreate(false);
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        getRequest().setExternalDatabase(sdxDatabaseRequest);
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
        Boolean clusterIsGiven = getTestContext().get(ClusterTestDto.class.getSimpleName()) == null;
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
        SdxInternalTestDto sdxInternalTestDto = withStackRequest(stack.getRequest());
        sdxInternalTestDto.withRuntimeVersion(commonClusterManagerProperties.getRuntimeVersion());
        return sdxInternalTestDto;
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

    public SdxInternalTestDto withEnableMultiAz(boolean enableMultiAz) {
        getRequest().setEnableMultiAz(enableMultiAz);
        return this;
    }

    public SdxInternalTestDto withEnableMultiAz() {
        getRequest().setEnableMultiAz(getCloudProvider().isMultiAZ());
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
        return when(sdxTestClient.refreshInternal(), key("refresh-sdx-" + getName()));
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
        AuditEventV4Responses auditEvents = AuditUtil.getAuditEvents(
                getTestContext().getMicroserviceClient(CloudbreakClient.class),
                CloudbreakEventService.DATALAKE_RESOURCE_TYPE,
                null,
                getResponse().getCrn());
        boolean hasSpotTermination = getResponse().getStackV4Response() != null && getResponse().getStackV4Response().getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetadata().stream())
                .anyMatch(metadata -> InstanceStatus.DELETED_BY_PROVIDER == metadata.getInstanceStatus());
        List<CDPStructuredEvent> structuredEvents = List.of();
        if (getResponse() != null && getResponse().getCrn() != null) {
            CDPStructuredEventV1Endpoint cdpStructuredEventV1Endpoint =
                    getTestContext().getMicroserviceClient(SdxClient.class).getDefaultClient().structuredEventsV1Endpoint();
            structuredEvents = StructuredEventUtil.getStructuredEvents(cdpStructuredEventV1Endpoint, getResponse().getCrn());
        }
        return new Clue(
                getResponse().getName(),
                getResponse().getCrn(),
                auditEvents,
                structuredEvents,
                getResponse(),
                hasSpotTermination);
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
}
