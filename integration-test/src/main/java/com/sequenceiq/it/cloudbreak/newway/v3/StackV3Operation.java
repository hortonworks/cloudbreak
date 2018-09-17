package com.sequenceiq.it.cloudbreak.newway.v3;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Action;
import com.sequenceiq.it.cloudbreak.newway.Assertion;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.GherkinTest;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackV3Operation extends AbstractCloudbreakEntity<StackScaleRequestV2, Response, StackV3Operation> {
    public static final String SCALE = "SCALE";

    protected StackV3Operation(String newId) {
        super(newId);
        setRequest(new StackScaleRequestV2());
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
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" scale " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV3Endpoint()
                        .putScalingInWorkspace(workspaceId, stack.getRequest().getGeneral().getName(), stackOperation.getRequest()));
    }

    public static void start(IntegrationTestContext integrationTestContext, Entity entity) {
        StackV3Operation stackOperation = (StackV3Operation) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" start " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV3Endpoint()
                        .putStartInWorkspace(workspaceId, stack.getRequest().getGeneral().getName()));
    }

    public static void stop(IntegrationTestContext integrationTestContext, Entity entity) {
        StackV3Operation stackOperation = (StackV3Operation) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" stop " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV3Endpoint()
                        .putStopInWorkspace(workspaceId, stack.getRequest().getGeneral().getName()));
    }

    public static void sync(IntegrationTestContext integrationTestContext, Entity entity) {
        StackV3Operation stackOperation = (StackV3Operation) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" sync " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV3Endpoint()
                        .putSyncInWorkspace(workspaceId, stack.getRequest().getGeneral().getName()));
    }

    public static StackV3Operation request() {
        return new StackV3Operation();
    }

    public static Action<StackV3Operation> scale() {
        return new Action<>(getTestContextStackOperation(), StackV3Operation::scale);
    }

    public static Action<StackV3Operation> start() {
        return new Action<>(getTestContextStackOperation(), StackV3Operation::start);
    }

    public static Action<StackV3Operation> stop() {
        return new Action<>(getTestContextStackOperation(), StackV3Operation::stop);
    }

    public static Action<StackV3Operation> sync() {
        return new Action<>(getTestContextStackOperation(), StackV3Operation::sync);
    }

    public static Assertion<StackV3Operation> assertThis(BiConsumer<StackV3Operation, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextStackOperation(GherkinTest.RESULT), check);
    }
}
