package com.sequenceiq.it.cloudbreak.newway.v3;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.ResourceAction;
import com.sequenceiq.it.cloudbreak.newway.Assertion;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.GherkinTest;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackV3Operation extends AbstractCloudbreakEntity<StackScaleV4Request, Response, StackV3Operation> {
    public static final String SCALE = "SCALE";

    protected StackV3Operation(String newId) {
        super(newId);
        setRequest(new StackScaleV4Request());
    }

    protected StackV3Operation() {
        this(SCALE);
    }

    public StackV3Operation withDesiredCount(int desiredCount) {
        getRequest().setDesiredCount(desiredCount);
        return this;
    }

    public StackV3Operation withGroupName(String groupName) {
        getRequest().setGroup(groupName);
        return this;
    }

    public StackV3Operation withStackId(Long x) {
        getRequest().setStackId(x);
        return this;
    }

    static Function<IntegrationTestContext, StackV3Operation> getTestContextStackOperation(String key) {
        return testContext -> testContext.getContextParam(key, StackV3Operation.class);
    }

    static Function<IntegrationTestContext, StackV3Operation> getTestContextStackOperation() {
        return getTestContextStackOperation(SCALE);
    }

    public static void scale(IntegrationTestContext integrationTestContext, Entity entity) {
        StackV3Operation stackOperation = (StackV3Operation) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        StackTestDto stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" scale " + stack.getRequest().getName());
        client.getCloudbreakClient().stackV4Endpoint().putScaling(workspaceId, stack.getRequest().getName(), stackOperation.getRequest());
    }

    public static void start(IntegrationTestContext integrationTestContext, Entity entity) {
        StackV3Operation stackOperation = (StackV3Operation) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        StackTestDto stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" start " + stack.getRequest().getName());
        client.getCloudbreakClient().stackV4Endpoint().putScaling(workspaceId, stack.getRequest().getName(), stackOperation.getRequest());
    }

    public static void stop(IntegrationTestContext integrationTestContext, Entity entity) {
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        StackTestDto stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" stop " + stack.getRequest().getName());
        client.getCloudbreakClient().stackV4Endpoint().putStop(workspaceId, stack.getRequest().getName());
    }

    public static void sync(IntegrationTestContext integrationTestContext, Entity entity) {
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        StackTestDto stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" sync " + stack.getRequest().getName());
        client.getCloudbreakClient().stackV4Endpoint().putSync(workspaceId, stack.getRequest().getName());
    }

    public static StackV3Operation request() {
        return new StackV3Operation();
    }

    public static ResourceAction<StackV3Operation> scale() {
        return new ResourceAction<>(getTestContextStackOperation(), StackV3Operation::scale);
    }

    public static ResourceAction<StackV3Operation> start() {
        return new ResourceAction<>(getTestContextStackOperation(), StackV3Operation::start);
    }

    public static ResourceAction<StackV3Operation> stop() {
        return new ResourceAction<>(getTestContextStackOperation(), StackV3Operation::stop);
    }

    public static ResourceAction<StackV3Operation> sync() {
        return new ResourceAction<>(getTestContextStackOperation(), StackV3Operation::sync);
    }

    public static Assertion<StackV3Operation> assertThis(BiConsumer<StackV3Operation, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextStackOperation(GherkinTest.RESULT), check);
    }
}
