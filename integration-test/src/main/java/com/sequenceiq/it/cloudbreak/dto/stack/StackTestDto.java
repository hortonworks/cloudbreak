package com.sequenceiq.it.cloudbreak.dto.stack;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest.STACK_DELETED;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.assign.Assignable;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Clue;
import com.sequenceiq.it.cloudbreak.context.Investigable;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.util.AuditUtil;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class StackTestDto extends StackTestDtoBase<StackTestDto> implements Purgable<StackV4Response, CloudbreakClient>, Searchable, Investigable, Assignable {

    public static final String STACK = "STACK";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTestDto.class);

    private GeneratedBlueprintV4Response generatedBlueprint;

    @Inject
    private StackTestClient stackTestClient;

    StackTestDto(String newId) {
        super(newId);
    }

    public StackTestDto() {
        this(STACK);
    }

    public StackTestDto(StackV4Request request) {
        this();
        setRequest(request);
    }

    public StackTestDto(TestContext testContext) {
        super(testContext);
    }

    @Override
    public StackTestDtoBase<StackTestDto> valid() {
        return super.valid().withEnvironmentClass(EnvironmentTestDto.class);
    }

    @Override
    public void cleanUp(TestContext context, MicroserviceClient client) {
        LOGGER.info("Cleaning up stack with name: {}", getName());
        if (getResponse() != null) {
            when(stackTestClient.forceDeleteV4(), key("delete-stack-" + getName()).withSkipOnFail(false));
            await(STACK_DELETED, new RunningParameter().withSkipOnFail(true));
        } else {
            LOGGER.info("Stack: {} response is null!", getName());
        }
    }

    @Override
    public List<StackV4Response> getAll(CloudbreakClient client) {
        StackV4Endpoint stackEndpoint = client.getDefaultClient().stackV4Endpoint();
        return stackEndpoint.list(client.getWorkspaceId(), null, false).getResponses().stream()
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
            client.getDefaultClient().stackV4Endpoint().delete(client.getWorkspaceId(), entity.getName(), true,
                    Crn.fromString(entity.getCrn()).getAccountId());
            testContext.await(this, STACK_DELETED, key("wait-purge-stack-" + entity.getName()));
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
        return when(stackTestClient.refreshV4(), key("refresh-stack-" + getName()));
    }

    @Override
    public CloudbreakTestDto wait(Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(desiredStatuses, runningParameter);
    }

    public StackTestDto withGeneratedBlueprint(GeneratedBlueprintV4Response generatedBlueprint) {
        this.generatedBlueprint = generatedBlueprint;
        return this;
    }

    public GeneratedBlueprintV4Response getGeneratedBlueprint() {
        return generatedBlueprint;
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
}
