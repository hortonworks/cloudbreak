package com.sequenceiq.it.cloudbreak.dto.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest.STACK_DELETED;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1SpotParameters;
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
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.util.AuditUtil;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.FlowUtil;

@Prototype
public class DistroXTestDto extends DistroXTestDtoBase<DistroXTestDto> implements Purgable<StackV4Response, CloudbreakClient>, Investigable, Searchable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXTestDto.class);

    private GeneratedBlueprintV4Response generatedBlueprint;

    private StackViewV4Response internalStackResponse;

    private GeneratedBlueprintV4Response generatedBlueprintV4Response;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private FlowUtil flowUtil;

    public DistroXTestDto(TestContext testContext) {
        super(new DistroXV1Request(), testContext);
    }

    @Override
    public DistroXTestDtoBase<DistroXTestDto> valid() {
        return super.valid().withEnvironment(EnvironmentTestDto.class);
    }

    @Override
    public int order() {
        return 400;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up distrox with name: {}", getName());
        if (getResponse() != null) {
            when(distroXTestClient.forceDelete(), key("delete-distrox-" + getName()).withSkipOnFail(false));
            awaitForFlow(key("delete-distrox-" + getName()));
            await(STACK_DELETED, new RunningParameter().withSkipOnFail(true));
        } else {
            LOGGER.info("Distrox: {} response is null!", getName());
        }
    }

    @Override
    public List<StackV4Response> getAll(CloudbreakClient client) {
        DistroXV1Endpoint distroXV1Endpoint = client.getCloudbreakClient().distroXV1Endpoint();
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
            client.getCloudbreakClient().distroXV1Endpoint().deleteByName(entity.getName(), true);
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
        return when(distroXTestClient.refresh(), RunningParameter.key("refresh-distrox-" + getName()).switchToAdmin());
    }

    @Override
    public CloudbreakTestDto wait(Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(desiredStatuses, runningParameter);
    }

    @Override
    public DistroXTestDto await(Map<String, Status> statuses) {
        super.await(statuses);
        waitTillFlowInOperation();
        return this;
    }

    public DistroXTestDto awaitAndIgnoreFlows(Map<String, Status> statuses) {
        super.await(statuses, emptyRunningParameter());
        return this;
    }

    @Override
    public DistroXTestDto await(Class<DistroXTestDto> entityClass, Map<String, Status> statuses) {
        super.await(entityClass, statuses);
        waitTillFlowInOperation();
        return this;
    }

    @Override
    public DistroXTestDto await(Class<DistroXTestDto> entityClass, Map<String, Status> statuses, Duration pollingInteval) {
        super.await(entityClass, statuses, pollingInteval);
        waitTillFlowInOperation();
        return this;
    }

    @Override
    public DistroXTestDto await(Class<DistroXTestDto> entityClass, Map<String, Status> statuses, RunningParameter runningParameter) {
        super.await(entityClass, statuses, runningParameter);
        waitTillFlowInOperation();
        return this;
    }

    @Override
    public DistroXTestDto await(Map<String, Status> statuses, RunningParameter runningParameter) {
        super.await(statuses, runningParameter);
        waitTillFlowInOperation();
        return this;
    }

    public DistroXTestDto awaitForInstance(Map<String, InstanceStatus> statuses) {
        return awaitForInstance(statuses, emptyRunningParameter());
    }

    public DistroXTestDto awaitForInstance(DistroXTestDto entity, Map<String, InstanceStatus> statuses, RunningParameter runningParameter) {
        return getTestContext().await(entity, statuses, runningParameter);
    }

    public DistroXTestDto awaitForInstance(Map<String, InstanceStatus> statuses, RunningParameter runningParameter) {
        return getTestContext().await(this, statuses, runningParameter);
    }

    public DistroXTestDto awaitForInstance(Map<String, InstanceStatus> statuses, RunningParameter runningParameter, Duration pollingInterval) {
        return getTestContext().await(this, statuses, runningParameter, pollingInterval);
    }

    public DistroXTestDto awaitForInstance(Map<String, InstanceStatus> statuses, Duration pollingInterval) {
        return awaitForInstance(statuses, emptyRunningParameter(), pollingInterval);
    }

    public DistroXTestDto awaitForFlow() {
        return awaitForFlow(emptyRunningParameter());
    }

    @Override
    public DistroXTestDto awaitForFlow(RunningParameter runningParameter) {
        return getTestContext().awaitForFlow(this, runningParameter);
    }

    private void waitTillFlowInOperation() {
        while (hasFlow()) {
            try {
                Thread.sleep(flowUtil.getPollingInterval());
            } catch (InterruptedException e) {
                LOGGER.warn("Exception has been occurred during wait for flow end: ", e);
            }
        }
    }

    private boolean hasFlow() {
        try {
            return ((CloudbreakClient) getTestContext().getAdminMicroserviceClient(CloudbreakClient.class))
                    .getCloudbreakClient()
                    .flowPublicEndpoint()
                    .hasFlowRunningByChainId(getLastKnownFlowChainId(), getCrn()).getHasActiveFlow();
        } catch (NotFoundException e) {
            return false;
        }
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

    public DistroXTestDto addTags(Map<String, String> tags) {
        getRequest().initAndGetTags().getUserDefined().putAll(tags);
        return this;
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
        boolean hasSpotTermination = getResponse().getInstanceGroups().stream()
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
}
