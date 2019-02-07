package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.withoutLogError;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.newway.action.StackRefreshAction;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.CloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackV4EntityBase;
import com.sequenceiq.it.cloudbreak.newway.v3.StackActionV4;

@Prototype
public class StackEntity extends StackV4EntityBase<StackEntity> implements Purgable<StackV4Response> {

    public static final String STACK = "STACK";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackEntity.class);

    StackEntity(String newId) {
        super(newId);
    }

    public StackEntity() {
        this(STACK);
    }

    public StackEntity(StackV4Request request) {
        this();
        setRequest(request);
    }

    public StackEntity(TestContext testContext) {
        super(testContext);
    }

    @Override
    public StackV4EntityBase<StackEntity> valid() {
        return super.valid().withEnvironment(EnvironmentEntity.class);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        when(StackActionV4::delete, withoutLogError());
        await(Status.DELETE_COMPLETED);
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
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(StackV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().stackV4Endpoint().delete(client.getWorkspaceId(), entity.getName(), true, false);
            wait(Status.DELETE_COMPLETED, key("wait-purge-stack-" + entity.getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), e.getMessage(), e);
        }
    }

    @Override
    public CloudbreakEntity refresh(TestContext context, CloudbreakClient cloudbreakClient) {
        return when(new StackRefreshAction(), key("refresh-stack-" + getName()));
    }

    @Override
    public CloudbreakEntity wait(Status desiredStatuses, RunningParameter runningParameter) {
        return await(desiredStatuses, runningParameter);
    }
}
