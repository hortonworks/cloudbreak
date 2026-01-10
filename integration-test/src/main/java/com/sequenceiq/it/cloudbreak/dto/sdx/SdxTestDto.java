package com.sequenceiq.it.cloudbreak.dto.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.util.Strings;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.common.model.Architecture;
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
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.util.AuditUtil;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.it.cloudbreak.util.LogCollectorUtil;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.StructuredEventUtil;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxAwsRequest;
import com.sequenceiq.sdx.api.model.SdxAwsSpotParameters;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;
import com.sequenceiq.sdx.api.model.SdxRecipe;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;

@Prototype
public class SdxTestDto extends AbstractSdxTestDto<SdxClusterRequest, SdxClusterDetailResponse, SdxTestDto> implements Purgable<SdxClusterResponse, SdxClient>,
        Investigable, Searchable {

    private static final String SDX_RESOURCE_NAME = "sdxName";

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxTestDto.class);

    private static final String DEFAULT_SDX_NAME = "test-sdx" + '-' + UUID.randomUUID().toString().replaceAll("-", "");

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private LogCollectorUtil logCollectorUtil;

    public SdxTestDto(TestContext testContex) {
        super(new SdxClusterRequest(), testContex);
    }

    @Override
    public SdxTestDto valid() {
        withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withEnvironmentName(getTestContext().get(EnvironmentTestDto.class).getResponse().getName())
                .withClusterShape(getCloudProvider().getClusterShape())
                .withTags(getCloudProvider().getTags())
                .withRuntimeVersion(getRequest().getImage() == null ? commonClusterManagerProperties.getRuntimeVersion() : null)
                .withEnableMultiAz(getCloudProvider().isMultiAZ())
                .withImageValidationCatalogAndImageIfPresent()
                .withEmbeddedDatabase();
        return getCloudProvider().sdx(this);
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
        return entity.getName().startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    @Override
    public void delete(TestContext testContext, SdxClusterResponse entity, SdxClient client) {
        String sdxName = entity.getName();
        try {
            client.getDefaultClient().sdxEndpoint().delete(sdxName, true);
            testContext.await(this, Map.of("status", DELETED), key("wait-purge-sdx-" + entity.getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on SDX {} purge. {}", sdxName, ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public Class<SdxClient> client() {
        return SdxClient.class;
    }

    @Override
    public SdxTestDto refresh() {
        LOGGER.info("Refresh SDX with name: {}", getName());
        return when(sdxTestClient.refresh(), key("refresh-sdx-" + getName()).withSkipOnFail(false).withLogError(false));
    }

    @Override
    public SdxTestDto wait(Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(desiredStatuses, runningParameter);
    }

    public SdxTestDto await(SdxClusterStatusResponse status) {
        return await(status, emptyRunningParameter());
    }

    public SdxTestDto await(SdxClusterStatusResponse status, RunningParameter runningParameter) {
        return getTestContext().await(this, Map.of("status", status), runningParameter);
    }

    public SdxTestDto awaitForFlow() {
        return awaitForFlow(emptyRunningParameter());
    }

    public SdxTestDto awaitForFlowFail() {
        return awaitForFlow(emptyRunningParameter().withWaitForFlowFail());
    }

    @Override
    public SdxTestDto awaitForFlow(RunningParameter runningParameter) {
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

    public SdxTestDto awaitForHealthyInstances() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                InstanceUtil.getInstanceStatusMapForStatus(getResponse().getStackV4Response(), InstanceStatus.SERVICES_HEALTHY));
        return awaitForInstance(instanceStatusMap);
    }

    public SdxTestDto awaitForDeletedInstancesOnProvider() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                getResponse().getStackV4Response().getInstanceGroups().stream().collect(Collectors.toMap(
                        instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream()
                                .map(InstanceMetaDataV4Response::getInstanceId).collect(Collectors.toList()),
                        instanceMetaDataV4Response -> InstanceStatus.DELETED_ON_PROVIDER_SIDE)));
        return awaitForInstance(instanceStatusMap);
    }

    public SdxTestDto awaitForHostGroups(List<String> hostGroups, InstanceStatus instanceStatus) {
        if (!getTestContext().getExceptionMap().isEmpty()) {
            Log.await(LOGGER, String.format("Await for instances is skipped because of previous error. Required status %s, host group(s) %s.",
                    instanceStatus, hostGroups));
            return this;
        }
        List<InstanceGroupV4Response> instanceGroups = getResponse().getStackV4Response().getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> hostGroups.contains(instanceGroupV4Response.getName()))
                .collect(Collectors.toList());
        if (hostGroups.size() == instanceGroups.size()) {
            List<String> instanceIds =
                    instanceGroups.stream().flatMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream())
                            .map(InstanceMetaDataV4Response::getInstanceId).collect(Collectors.toList());
            return awaitForInstance(Map.of(instanceIds, instanceStatus));
        } else {
            throw new IllegalStateException("Can't find instance groups with this name: " + hostGroups);
        }
    }

    public SdxTestDto withCloudStorage() {
        SdxCloudStorageTestDto cloudStorage = getCloudProvider().cloudStorage(given(SdxCloudStorageTestDto.class));
        if (cloudStorage == null) {
            throw new IllegalArgumentException("SDX Cloud Storage does not exist!");
        }
        return withCloudStorage(cloudStorage.getRequest());
    }

    public SdxTestDto withCloudStorage(SdxCloudStorageRequest cloudStorage) {
        getRequest().setCloudStorage(cloudStorage);
        return this;
    }

    public SdxTestDto withRangerRazEnabled(boolean rangerRazEnabled) {
        getRequest().setEnableRangerRaz(rangerRazEnabled);
        return this;
    }

    public SdxTestDto withTags(Map<String, String> tags) {
        getRequest().addTags(tags);
        return this;
    }

    public SdxTestDto withClusterShape(SdxClusterShape shape) {
        getRequest().setClusterShape(shape);
        return this;
    }

    public SdxTestDto withSpotPercentage(int spotPercentage) {
        SdxAwsRequest aws = getRequest().getAws();
        if (Objects.isNull(aws)) {
            aws = new SdxAwsRequest();
            getRequest().setAws(aws);
        }
        SdxAwsSpotParameters spot = new SdxAwsSpotParameters();
        spot.setPercentage(spotPercentage);
        aws.setSpot(spot);

        return this;
    }

    public SdxTestDto withExternalDatabase(SdxDatabaseRequest database) {
        if (isDatabaseAvailabilityTypeNone(database)) {
            throw new TestFailException("Datalake's database availability type is NONE inside withExternalDatabase() function");
        }
        getRequest().setExternalDatabase(database);
        return this;
    }

    public SdxTestDto withEmbeddedDatabase(SdxDatabaseRequest database) {
        if (!isDatabaseAvailabilityTypeNone(database)) {
            throw new TestFailException("Datalake's database availability type is not NONE inside withEmbeddedDatabase() function");
        }
        getRequest().setExternalDatabase(database);
        return this;
    }

    public SdxTestDto withEmbeddedDatabase() {
        if (getRequest().getExternalDatabase() == null) {
            SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
            sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
            sdxDatabaseRequest.setCreate(false);
            getRequest().setExternalDatabase(sdxDatabaseRequest);
        }
        return this;
    }

    private boolean isDatabaseAvailabilityTypeNone(SdxDatabaseRequest database) {
        return database.getAvailabilityType() == SdxDatabaseAvailabilityType.NONE;
    }

    public SdxTestDto withEnvironment() {
        EnvironmentTestDto environment = getTestContext().given(EnvironmentTestDto.class);
        if (environment == null) {
            throw new IllegalArgumentException(String.format("Environment has not been provided for this Sdx: '%s' response!", getName()));
        }
        return withEnvironmentName(environment.getResponse().getName());
    }

    public SdxTestDto withEnvironmentClass(Class<EnvironmentTestDto> environmentClass) {
        EnvironmentTestDto environment = getTestContext().get(environmentClass.getSimpleName());
        if (environment == null) {
            throw new IllegalArgumentException(String.format("Environment has not been provided for this Sdx: '%s' response!", getName()));
        }
        return withEnvironmentName(environment.getResponse().getName());
    }

    public SdxTestDto withEnvironmentName(String environmentName) {
        getRequest().setEnvironment(environmentName);
        return this;
    }

    public SdxTestDto withName(String name) {
        setName(name);
        return this;
    }

    public SdxTestDto withRecipe(String recipeName, String hostGroup) {
        if (CollectionUtils.isEmpty(getRequest().getRecipes())) {
            getRequest().setRecipes(new HashSet<>());
        }
        Set<SdxRecipe> recipes = getRequest().getRecipes();
        SdxRecipe sdxRecipe = new SdxRecipe();
        sdxRecipe.setName(recipeName);
        sdxRecipe.setHostGroup(hostGroup);
        recipes.add(sdxRecipe);
        return this;
    }

    public SdxTestDto withRecipes(Set<String> recipeNames, String hostGroup) {
        Set<SdxRecipe> sdxRecipes = getRequest().getRecipes();
        Set<SdxRecipe> newRecipes = new HashSet<>();

        for (String recipeName : recipeNames) {
            SdxRecipe sdxRecipe = new SdxRecipe();
            sdxRecipe.setName(recipeName);
            sdxRecipe.setHostGroup(hostGroup);
            newRecipes.add(sdxRecipe);
        }

        if (CollectionUtils.isEmpty(sdxRecipes)) {
            getRequest().setRecipes(newRecipes);
        } else {
            sdxRecipes.addAll(newRecipes);
            getRequest().setRecipes(sdxRecipes);
        }
        return this;
    }

    public SdxTestDto withCustomInstanceGroup(String instanceGroup, String instanceType) {
        if (getRequest().getCustomInstanceGroups() == null) {
            getRequest().setCustomInstanceGroups(new ArrayList<>());
        }
        List<SdxInstanceGroupRequest> customInstanceGroups = getRequest().getCustomInstanceGroups();
        SdxInstanceGroupRequest customInstanceGroup = new SdxInstanceGroupRequest();
        customInstanceGroup.setName(instanceGroup);
        customInstanceGroup.setInstanceType(instanceType);
        customInstanceGroups.add(customInstanceGroup);
        return this;
    }

    @Override
    public String getResourceNameType() {
        return SDX_RESOURCE_NAME;
    }

    @Override
    public String getName() {
        return super.getName() == null ? DEFAULT_SDX_NAME : super.getName();
    }

    @Override
    public String getSearchId() {
        return getName();
    }

    @Override
    public String getCrn() {
        if (getResponse() == null) {
            throw new IllegalStateException("You have tried to assign to SdxTestDto," +
                    " that hasn't been created and therefore has no Response object yet.");
        }
        return getResponse().getCrn();
    }

    public SdxRepairRequest getSdxRepairRequest(String... hostgroups) {
        SdxRepairTestDto repair = getCloudProvider().sdxRepair(given(SdxRepairTestDto.class).withHostGroupNames(Arrays.asList(hostgroups)));
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

    public SdxUpgradeDatabaseServerRequest getSdxUpgradeDatabaseServerRequest() {
        SdxUpgradeDatabaseServerTestDto upgradeDatabaseServer = given(SdxUpgradeDatabaseServerTestDto.class);
        if (upgradeDatabaseServer == null) {
            throw new IllegalArgumentException("SDX Upgrade Database Server does not exist!");
        }
        return upgradeDatabaseServer.getRequest();
    }

    public SdxRecoveryRequest getSdxRecoveryRequest() {
        SdxRecoveryTestDto recovery = given(SdxRecoveryTestDto.class);
        if (recovery == null) {
            throw new IllegalArgumentException("SDX Recovery does not exist!");
        }
        return recovery.getRequest();
    }

    public SdxTestDto withRuntimeVersion(String runtimeVersion) {
        getRequest().setRuntime(runtimeVersion);
        getRequest().setImage(null);
        return this;
    }

    public SdxTestDto withArchitecture(Architecture architecture) {
        getRequest().setArchitecture(architecture.toString().toUpperCase(Locale.ROOT));
        getRequest().setImage(null);
        return this;
    }

    public SdxTestDto withEnableMultiAz(boolean enableMultiAz) {
        getRequest().setEnableMultiAz(enableMultiAz);
        return this;
    }

    public SdxTestDto withEnableMultiAz() {
        getRequest().setEnableMultiAz(getCloudProvider().isMultiAZ());
        return this;
    }

    public SdxTestDto withImageId(String imageId) {
        ImageSettingsV4Request imageRequest = new ImageSettingsV4Request();
        imageRequest.setId(imageId);
        getRequest().setImage(imageRequest);
        getRequest().setRuntime(null);
        return this;
    }

    public SdxTestDto withOs(String os) {
        if (Strings.isNotNullAndNotEmpty(os)) {
            ImageSettingsV4Request imageRequest = new ImageSettingsV4Request();
            imageRequest.setOs(os);
            getRequest().setImage(imageRequest);
        }
        return this;
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
        collectLogFiles(stackResponse);
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

    private void collectLogFiles(StackV4Response stackResponse) {
        Multimap<String, Pair<String, String>> collectedLogFiles = ArrayListMultimap.create();
        try {
            List<String> ipAddresses = stackResponse.getInstanceGroups().stream().flatMap(ig -> ig.getMetadata().stream())
                    .map(imd -> imd.getPublicIp() != null && !Objects.equals(imd.getPublicIp(), "N/A") ? imd.getPublicIp() : imd.getPrivateIp()).toList();
            logCollectorUtil.collectLogFiles(stackResponse.getStatusReason(), ipAddresses);
        } catch (Exception e) {
            LOGGER.warn("Failed to collect datalake log files for investigation.", e);
        }
    }

    @Override
    public void deleteForCleanup() {
        try {
            LOGGER.info("Deleting DataLake with crn: {}", getCrn());
            setFlow("SDX deletion", getClientForCleanup().getDefaultClient().sdxEndpoint().deleteByCrn(getCrn(), true));
            awaitForFlow();
        } catch (NotFoundException nfe) {
            LOGGER.info("SDX resource not found, thus cleanup not needed.");
        }
    }

    public SdxTestDto withMarketplaceUpgradeCatalogAndImage(String imgCatalogKey) {
        return withImage(imgCatalogKey, getCloudProvider().getSdxMarketplaceUpgradeImageId());
    }

    public SdxTestDto withImageValidationCatalogAndImageIfPresent() {
        return withImage(commonCloudProperties.getImageValidation().getSourceCatalogName(), commonCloudProperties.getImageValidation().getImageUuid());
    }

    public SdxTestDto withImage(String imageCatalog, String imageUuid) {
        if (!Strings.isNullOrEmpty(imageCatalog) && !Strings.isNullOrEmpty(imageUuid)) {
            LOGGER.info("Using catalog [{}] and image [{}] for creating SDX", imageCatalog, imageUuid);
            ImageSettingsV4Request imageSettingsRequest = new ImageSettingsV4Request();
            imageSettingsRequest.setCatalog(imageCatalog);
            imageSettingsRequest.setId(imageUuid);

            withRuntimeVersion(null);
            getRequest().setImage(imageSettingsRequest);
        } else {
            LOGGER.warn("Catalog [{}] or image [{}] is null or empty", imageCatalog, imageUuid);
        }
        return this;
    }
}