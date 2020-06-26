package com.sequenceiq.it.cloudbreak.dto.stack;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.withoutLogError;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest.STACK_DELETED;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
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
public class StackTestDto extends StackTestDtoBase<StackTestDto> implements Purgable<StackV4Response, CloudbreakClient>, Searchable, Investigable {

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
        return super.valid().withEnvironment(EnvironmentTestDto.class);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        when(stackTestClient.forceDeleteV4(), withoutLogError());
        await(STACK_DELETED);
    }

    @Override
    public List<StackV4Response> getAll(CloudbreakClient client) {
        StackV4Endpoint stackEndpoint = client.getCloudbreakClient().stackV4Endpoint();
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
            client.getCloudbreakClient().stackV4Endpoint().delete(client.getWorkspaceId(), entity.getName(), true);
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
    public CloudbreakTestDto refresh(TestContext context, CloudbreakClient cloudbreakClient) {
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
