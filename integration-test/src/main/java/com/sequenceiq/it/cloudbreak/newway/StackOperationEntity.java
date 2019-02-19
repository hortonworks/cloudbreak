package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackOperationEntity extends AbstractCloudbreakEntity<StackScaleV4Request, Response, StackOperationEntity> {

    private static final String SCALE = "SCALE";

    protected StackOperationEntity(String newId) {
        super(newId);
        setRequest(new StackScaleV4Request());
    }

    protected StackOperationEntity() {
        this(SCALE);
    }

    public static StackOperationEntity request() {
        return new StackOperationEntity();
    }

    public static ResourceAction<StackOperationEntity> scale() {
        return new ResourceAction<>(getTestContextStackOperation(), StackOperationEntity::scale);
    }

    public static ResourceAction<StackOperationEntity> start() {
        return new ResourceAction<>(getTestContextStackOperation(), StackOperationEntity::start);
    }

    public static ResourceAction<StackOperationEntity> stop() {
        return new ResourceAction<>(getTestContextStackOperation(), StackOperationEntity::stop);
    }

    public static Assertion<StackOperationEntity> assertThis(BiConsumer<StackOperationEntity, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextStackOperation(GherkinTest.RESULT), check);
    }

    public StackOperationEntity withDesiredCount(int desiredCount) {
        getRequest().setDesiredCount(desiredCount);
        return this;
    }

    public StackOperationEntity withGroupName(String groupName) {
        getRequest().setGroup(groupName);
        return this;
    }

    static Function<IntegrationTestContext, StackOperationEntity> getTestContextStackOperation(String key) {
        return testContext -> testContext.getContextParam(key, StackOperationEntity.class);
    }

    static Function<IntegrationTestContext, StackOperationEntity> getTestContextStackOperation() {
        return getTestContextStackOperation(SCALE);
    }

    public static void start(IntegrationTestContext integrationTestContext, Entity entity) {
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackTestDto stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" start " + stack.getRequest().getName());
        client.getCloudbreakClient().stackV4Endpoint().putStart(client.getWorkspaceId(), stack.getRequest().getName());
    }

    public static void stop(IntegrationTestContext integrationTestContext, Entity entity) {
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackTestDto stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" stop " + stack.getRequest().getName());
        client.getCloudbreakClient().stackV4Endpoint().putStop(client.getWorkspaceId(), stack.getRequest().getName());
    }

    private static void scale(IntegrationTestContext integrationTestContext, Entity entity) {
        StackOperationEntity stackOperation = (StackOperationEntity) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackTestDto stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" scale " + stack.getRequest().getName());
        client.getCloudbreakClient().stackV4Endpoint().putScaling(client.getWorkspaceId(), stack.getRequest().getName(), stackOperation.getRequest());
    }
}
