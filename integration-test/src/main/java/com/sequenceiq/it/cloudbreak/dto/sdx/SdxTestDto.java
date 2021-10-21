package com.sequenceiq.it.cloudbreak.dto.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Clue;
import com.sequenceiq.it.cloudbreak.context.Investigable;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.util.AuditUtil;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxAwsRequest;
import com.sequenceiq.sdx.api.model.SdxAwsSpotParameters;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;
import com.sequenceiq.sdx.api.model.SdxRecipe;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;

@Prototype
public class SdxTestDto extends AbstractSdxTestDto<SdxClusterRequest, SdxClusterDetailResponse, SdxTestDto> implements Purgable<SdxClusterResponse, SdxClient>,
        Investigable {

    private static final String SDX_RESOURCE_NAME = "sdxName";

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxTestDto.class);

    private static final String DEFAULT_SDX_NAME = "test-sdx" + '-' + UUID.randomUUID().toString().replaceAll("-", "");

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    public SdxTestDto(TestContext testContex) {
        super(new SdxClusterRequest(), testContex);
    }

    @Override
    public SdxTestDto valid() {
        withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withEnvironmentName(getTestContext().get(EnvironmentTestDto.class).getResponse().getName())
                .withClusterShape(getCloudProvider().getClusterShape())
                .withTags(getCloudProvider().getTags())
                .withRuntimeVersion(commonClusterManagerProperties.getRuntimeVersion());
        return getCloudProvider().sdx(this);
    }

    @Override
    public void cleanUp(TestContext context, MicroserviceClient client) {
        LOGGER.info("Cleaning up sdx with name: {}", getName());
        if (getResponse() != null) {
            when(sdxTestClient.forceDelete(), key("delete-sdx-" + getName()).withSkipOnFail(false));
            await(DELETED, new RunningParameter().withSkipOnFail(true));
        } else {
            LOGGER.info("Sdx: {} response is null!", getName());
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
        return when(sdxTestClient.refresh(), key("refresh-sdx-" + getName()));
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

    public SdxTestDto await(SdxClusterStatusResponse status, RunningParameter runningParameter, Duration pollingInterval) {
        return getTestContext().await(this, Map.of("status", status), runningParameter, pollingInterval);
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
                InstanceUtil.getInstanceStatusMap(getResponse().getStackV4Response()));
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

    public SdxTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses) {
        return awaitForInstance(statuses, emptyRunningParameter());
    }

    public SdxTestDto awaitForInstance(SdxTestDto entity, Map<List<String>, InstanceStatus> statuses, RunningParameter runningParameter) {
        return getTestContext().awaitForInstance(entity, statuses, runningParameter);
    }

    public SdxTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses, RunningParameter runningParameter) {
        return getTestContext().awaitForInstance(this, statuses, runningParameter);
    }

    public SdxTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses, RunningParameter runningParameter, Duration pollingInterval) {
        return getTestContext().awaitForInstance(this, statuses, runningParameter, pollingInterval);
    }

    public SdxTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses, Duration pollingInterval) {
        return awaitForInstance(statuses, emptyRunningParameter(), pollingInterval);
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
        getRequest().setExternalDatabase(database);
        return this;
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

    private SdxTestDto withEnvironmentDto(EnvironmentTestDto environmentTestDto) {
        return withEnvironmentName(environmentTestDto.getResponse().getName());
    }

    public SdxTestDto withEnvironmentKey(RunningParameter environmentKey) {
        EnvironmentTestDto env = getTestContext().get(environmentKey.getKey());
        if (env == null) {
            throw new IllegalArgumentException(String.format("Environment has not been provided for this Sdx: '%s' response!", getName()));
        }
        return withEnvironmentName(env.getResponse().getName());
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

    @Override
    public String getResourceNameType() {
        return SDX_RESOURCE_NAME;
    }

    @Override
    public String getName() {
        return super.getName() == null ? DEFAULT_SDX_NAME : super.getName();
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

    public SdxRecoveryRequest getSdxRecoveryRequest() {
        SdxRecoveryTestDto recovery = given(SdxRecoveryTestDto.class);
        if (recovery == null) {
            throw new IllegalArgumentException("SDX Recovery does not exist!");
        }
        return recovery.getRequest();
    }

    public SdxClusterResizeRequest getSdxResizeRequest() {
        SdxResizeTestDto resize = given(SdxResizeTestDto.class);
        if (resize == null) {
            throw new IllegalArgumentException("SDX Resize does not exist!");
        }
        return resize.getRequest();
    }

    public SdxTestDto withRuntimeVersion(String runtimeVersion) {
        getRequest().setRuntime(runtimeVersion);
        return this;
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
}
