package com.sequenceiq.it.cloudbreak.dto.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest.STACK_DELETED;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.apache.commons.collections4.ListUtils;
import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1SpotParameters;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Clue;
import com.sequenceiq.it.cloudbreak.context.Investigable;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.util.AuditUtil;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class DistroXTestDto extends DistroXTestDtoBase<DistroXTestDto> implements Purgable<StackV4Response, CloudbreakClient>, Investigable, Searchable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXTestDto.class);

    private GeneratedBlueprintV4Response generatedBlueprint;

    private StackViewV4Response internalStackResponse;

    private List<String> removableInstanceIds;

    private String initiatorUserCrn;

    @Inject
    private DistroXTestClient distroXTestClient;

    public DistroXTestDto(TestContext testContext) {
        super(new DistroXV1Request(), testContext);
    }

    @Override
    public DistroXTestDtoBase<DistroXTestDto> valid() {
        DistroXTestDtoBase<DistroXTestDto> valid = super.valid();
        EnvironmentTestDto environmentTestDto = getTestContext().get(EnvironmentTestDto.class);
        if (environmentTestDto != null && environmentTestDto.getResponse() != null) {
            valid.withEnvironmentName(environmentTestDto.getName());
        }
        return valid;
    }

    @Override
    public int order() {
        return 400;
    }

    @Override
    public void deleteForCleanup() {
        try {
            CloudbreakClient clientForCleanup = getClientForCleanup();
            clientForCleanup.getDefaultClient().distroXV1Endpoint().deleteByCrn(getCrn(), true);
            awaitWithClient(STACK_DELETED, clientForCleanup);
        } catch (NotFoundException nfe) {
            LOGGER.info("resource not found, thus cleanup not needed.");
        }
    }

    @Override
    public List<StackV4Response> getAll(CloudbreakClient client) {
        DistroXV1Endpoint distroXV1Endpoint = client.getDefaultClient().distroXV1Endpoint();
        return distroXV1Endpoint.list(null, null).getResponses().stream()
                .filter(s -> s.getName() != null)
                .map(s -> {
                    StackV4Response stackResponse = new StackV4Response();
                    stackResponse.setName(s.getName());
                    return stackResponse;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean deletable(StackV4Response entity) {
        return entity.getName().startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    @Override
    public void delete(TestContext testContext, StackV4Response entity, CloudbreakClient client) {
        try {
            client.getDefaultClient().distroXV1Endpoint().deleteByName(entity.getName(), true);
            testContext.await(this, STACK_DELETED, key("wait-purge-distrox-" + entity.getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public Class<CloudbreakClient> client() {
        return CloudbreakClient.class;
    }

    @Override
    public CloudbreakTestDto refresh() {
        return when(distroXTestClient.refresh(), RunningParameter.key("refresh-distrox-" + getName()));
    }

    @Override
    public CloudbreakTestDto wait(Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(desiredStatuses, runningParameter);
    }

    @Override
    public DistroXTestDto await(Map<String, Status> statuses) {
        return getTestContext().await(this, statuses);
    }

    public DistroXTestDto awaitAndIgnoreFlows(Map<String, Status> statuses) {
        super.await(statuses, emptyRunningParameter());
        return this;
    }

    @Override
    public DistroXTestDto await(Class<DistroXTestDto> entityClass, Map<String, Status> statuses) {
        return getTestContext().await(this, statuses, emptyRunningParameter());
    }

    @Override
    public DistroXTestDto await(Class<DistroXTestDto> entityClass, Map<String, Status> statuses, RunningParameter runningParameter) {
        return getTestContext().await(this, statuses, runningParameter);
    }

    @Override
    public DistroXTestDto await(Map<String, Status> statuses, RunningParameter runningParameter) {
        return getTestContext().await(this, statuses, runningParameter);
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
        return getResponse() != null && getResponse().getInstanceGroups() != null;
    }

    public DistroXTestDto awaitForHealthyInstances() {
        Map<List<String>, InstanceStatus> instanceStatusMap = getInstanceStatusMapIfAvailableInResponse(() ->
                InstanceUtil.getInstanceStatusMap(getResponse()));
        return awaitForInstance(instanceStatusMap);
    }

    public DistroXTestDto awaitForRemovableInstancesByState(InstanceStatus instanceStatus) {
        if (!getRemovableInstanceIds().isEmpty()) {
            return awaitForInstance(Map.of(getRemovableInstanceIds(), instanceStatus));
        } else {
            throw new IllegalStateException(String.format("There is no '%s' instance to wait!", instanceStatus));
        }
    }

    public DistroXTestDto awaitForHostGroup(String hostGroup, InstanceStatus instanceStatus) {
        Optional<InstanceGroupV4Response> instanceGroup = getResponse().getInstanceGroups().stream()
                .filter(instanceGroupV4Response -> hostGroup.equals(instanceGroupV4Response.getName()))
                .filter(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream()
                        .anyMatch(instanceMetaDataV4Response -> Objects.nonNull(instanceMetaDataV4Response.getInstanceId())))
                .findAny();
        if (instanceGroup.isPresent()) {
            List<String> instanceIds = instanceGroup.get().getMetadata().stream()
                    .filter(instanceMetaDataV4Response -> Objects.nonNull(instanceMetaDataV4Response.getInstanceId()))
                    .map(InstanceMetaDataV4Response::getInstanceId)
                    .collect(Collectors.toList());
            return awaitForInstance(Map.of(instanceIds, instanceStatus));
        } else {
            throw new IllegalStateException("Can't find valid instance group with this name: " + hostGroup);
        }
    }

    public DistroXTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses) {
        return awaitForInstance(statuses, emptyRunningParameter());
    }

    public DistroXTestDto awaitForInstance(Map<List<String>, InstanceStatus> statuses, RunningParameter runningParameter) {
        return getTestContext().awaitForInstance(this, statuses, runningParameter);
    }

    public DistroXTestDto awaitForFlow() {
        return awaitForFlow(emptyRunningParameter());
    }

    @Override
    public DistroXTestDto awaitForFlow(RunningParameter runningParameter) {
        return getTestContext().awaitForFlow(this, runningParameter);
    }

    public DistroXTestDto withGeneratedBlueprint(GeneratedBlueprintV4Response generatedBlueprint) {
        this.generatedBlueprint = generatedBlueprint;
        return this;
    }

    public GeneratedBlueprintV4Response getGeneratedBlueprint() {
        return generatedBlueprint;
    }

    public DistroXTestDto fromClusterDefinition(String key) {
        Optional<ClusterTemplateTestDto> template = Optional.ofNullable(getTestContext().get(key));
        setRequest(template.orElseThrow(() -> new TestFailException("Unable to find DistroXV1Request")).getRequest().getDistroXTemplate());
        return this;
    }

    public StackViewV4Response getInternalStackResponse() {
        return internalStackResponse;
    }

    public DistroXTestDto withInternalStackResponse(StackViewV4Response internalStackResponse) {
        this.internalStackResponse = internalStackResponse;
        return this;
    }

    public DistroXTestDto withGeneratedBlueprintV4Response(GeneratedBlueprintV4Response response) {
        this.generatedBlueprint = response;
        return this;
    }

    public DistroXTestDto withTemplate(String template) {
        getRequest().getCluster().setBlueprintName(template);
        return this;
    }

    public DistroXTestDto withSpotPercentage(int spotPercentage) {
        getRequest().getInstanceGroups().stream()
                .map(InstanceGroupV1Request::getTemplate)
                .forEach(instanceTemplateV1Request -> {
                    AwsInstanceTemplateV1Parameters aws = instanceTemplateV1Request.getAws();
                    if (Objects.isNull(aws)) {
                        aws = new AwsInstanceTemplateV1Parameters();
                        instanceTemplateV1Request.setAws(aws);
                    }
                    AwsInstanceTemplateV1SpotParameters spot = new AwsInstanceTemplateV1SpotParameters();
                    spot.setPercentage(spotPercentage);
                    aws.setSpot(spot);
                });
        return this;
    }

    public DistroXTestDto withRecipe(String recipeName) {
        InstanceGroupV1Request instanceGroupV1Request = getRequest().getInstanceGroups().iterator().next();
        instanceGroupV1Request.setRecipeNames(Sets.newHashSet());
        instanceGroupV1Request.getRecipeNames().add(recipeName);
        return this;
    }

    public DistroXTestDto withInitiatorUserCrn(String initiatorUserCrn) {
        this.initiatorUserCrn = initiatorUserCrn;
        return this;
    }

    public DistroXTestDto withExternalDatabaseOnAws(DistroXDatabaseRequest database) {
        if (CloudPlatform.AWS.equals(getCloudPlatform())) {
            getRequest().setExternalDatabase(database);
        }
        return this;
    }

    public String getInitiatorUserCrn() {
        return initiatorUserCrn;
    }

    public DistroXTestDto addTags(Map<String, String> tags) {
        getRequest().initAndGetTags().getUserDefined().putAll(tags);
        return this;
    }

    public DistroXUpgradeV1Request getDistroXUpgradeRequest() {
        DistroXUpgradeTestDto upgradeTestDto = given(DistroXUpgradeTestDto.class);
        if (upgradeTestDto == null) {
            throw new IllegalArgumentException("DistroX upgrade dto does not exist!");
        }
        return upgradeTestDto.getRequest();
    }

    @Override
    public Clue investigate() {
        if (getResponse() == null || getResponse().getId() == null) {
            return null;
        }
        AuditEventV4Responses auditEvents = AuditUtil.getAuditEvents(
                getTestContext().getMicroserviceClient(CloudbreakClient.class),
                CloudbreakEventService.DATAHUB_RESOURCE_TYPE,
                getResponse().getId(),
                null);
        boolean hasSpotTermination = (getResponse().getInstanceGroups() == null) ? false : getResponse().getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetadata().stream())
                .anyMatch(metadata -> InstanceStatus.DELETED_BY_PROVIDER == metadata.getInstanceStatus());
        return new Clue("DistroX", auditEvents, getResponse(), hasSpotTermination);
    }

    @Override
    public String getSearchId() {
        return getName();
    }

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }

    public void setRemovableInstanceIds(List<String> removableInstanceIds) {
        this.removableInstanceIds = removableInstanceIds;
    }

    public List<String> getRemovableInstanceIds() {
        return ListUtils.emptyIfNull(removableInstanceIds);
    }
}