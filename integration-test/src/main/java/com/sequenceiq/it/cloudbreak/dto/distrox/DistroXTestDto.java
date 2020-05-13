package com.sequenceiq.it.cloudbreak.dto.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.withoutLogError;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest.STACK_DELETED;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1SpotParameters;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
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
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;

@Prototype
public class DistroXTestDto extends DistroXTestDtoBase<DistroXTestDto> implements Purgable<StackV4Response, CloudbreakClient>, Investigable, Searchable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXTestDto.class);

    private GeneratedBlueprintV4Response generatedBlueprint;

    private StackViewV4Response internalStackResponse;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private WaitUtil waitUtil;

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
        LOGGER.info("Cleaning up resource with name: {}", getName());
        when(distroXTestClient.forceDelete(), withoutLogError());
        await(STACK_DELETED);
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
    public CloudbreakTestDto refresh(TestContext context, CloudbreakClient cloudbreakClient) {
        return when(distroXTestClient.refresh(), key("refresh-distrox-" + getName()));
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
    public DistroXTestDto await(Class<DistroXTestDto> entityClass, Map<String, Status> statuses, long pollingInteval) {
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

    private void waitTillFlowInOperation() {
        while (hasFlow()) {
            try {
                Thread.sleep(waitUtil.getPollingInterval());
            } catch (InterruptedException e) {
                LOGGER.warn("Exception has been occurred during wait for flow end: ", e);
            }
        }
    }

    private boolean hasFlow() {
        try {
            return getTestContext().getCloudbreakClient().getCloudbreakClient()
                    .flowEndpoint()
                    .getFlowLogsByResourceName(getName()).stream().anyMatch(flowentry -> !flowentry.getFinalized());
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

    @Override
    public String investigate() {
        if (getResponse() == null || getResponse().getId() == null) {
            return null;
        }
        Long id = getResponse().getId();

        return "DistroX audit events: " + AuditUtil.getAuditEvents(getTestContext().getCloudbreakClient(), "stacks", id, null);
    }

    @Override
    public String getSearchId() {
        return getName();
    }
}
