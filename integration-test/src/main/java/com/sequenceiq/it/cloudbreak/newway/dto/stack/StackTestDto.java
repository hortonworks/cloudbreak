package com.sequenceiq.it.cloudbreak.newway.dto.stack;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.withoutLogError;
import static com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest.STACK_DELETED;
import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.v4.StackActionV4;

@Prototype
public class StackTestDto extends StackTestDtoBase<StackTestDto> implements Purgable<StackV4Response> {

    public static final String STACK = "STACK";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTestDto.class);

    private GeneratedClusterDefinitionV4Response generatedClusterDefinition;

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
        when(StackActionV4::delete, withoutLogError());
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
        return entity.getName().startsWith(resourceProperyProvider().prefix());
    }

    @Override
    public void delete(TestContext testContext, StackV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().stackV4Endpoint().delete(client.getWorkspaceId(), entity.getName(), true, false);
            testContext.await(this, STACK_DELETED, key("wait-purge-stack-" + entity.getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), getErrorMessage(e), e);
        }
    }

    @Override
    public CloudbreakTestDto refresh(TestContext context, CloudbreakClient cloudbreakClient) {
        return when(stackTestClient.refreshV4(), key("refresh-stack-" + getName()));
    }

    @Override
    public CloudbreakTestDto wait(Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(desiredStatuses, runningParameter);
    }

    public StackTestDto withGeneratedClusterDefinition(GeneratedClusterDefinitionV4Response generatedClusterDefinition) {
        this.generatedClusterDefinition = generatedClusterDefinition;
        return this;
    }

    public GeneratedClusterDefinitionV4Response getGeneratedClusterDefinition() {
        return generatedClusterDefinition;
    }
}
