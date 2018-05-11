package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackOperation extends AbstractCloudbreakEntity<StackScaleRequestV2, Response> {
    public static final String SCALE = "SCALE";

    protected StackOperation(String newId) {
        super(newId);
        setRequest(new StackScaleRequestV2());
    }

    protected StackOperation() {
        this(SCALE);
    }

    public StackOperation withDesiredCount(int desiredCount) {
        getRequest().setDesiredCount(desiredCount);
        return this;
    }

    public StackOperation withGroupName(String groupName) {
        getRequest().setGroup(groupName);
        return this;
    }

    public StackOperation withStackId(Long x) {
        getRequest().setStackId(x);
        return this;
    }

    static Function<IntegrationTestContext, StackOperation> getTestContextStackOperation(String key) {
        return (testContext) -> testContext.getContextParam(key, StackOperation.class);
    }

    static Function<IntegrationTestContext, StackOperation> getTestContextStackOperation() {
        return getTestContextStackOperation(SCALE);
    }

    public static void scale(IntegrationTestContext integrationTestContext, Entity entity) {
        StackOperation stackOperation = (StackOperation) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" scale " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV2Endpoint()
                        .putScaling(stack.getRequest().getGeneral().getName(), stackOperation.getRequest()));
    }

    public static void start(IntegrationTestContext integrationTestContext, Entity entity) {
        StackOperation stackOperation = (StackOperation) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" start " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV2Endpoint()
                        .putStart(stack.getRequest().getGeneral().getName()));
    }

    public static void stop(IntegrationTestContext integrationTestContext, Entity entity) {
        StackOperation stackOperation = (StackOperation) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" stop " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV2Endpoint()
                        .putStop(stack.getRequest().getGeneral().getName()));
    }

    public static void sync(IntegrationTestContext integrationTestContext, Entity entity) {
        StackOperation stackOperation = (StackOperation) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" sync " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV2Endpoint()
                        .putSync(stack.getRequest().getGeneral().getName()));
    }

    public static StackOperation request() {
        return new StackOperation();
    }

    public static Action<StackOperation> scale() {
        return new Action<>(getTestContextStackOperation(), StackOperation::scale);
    }

    public static Action<StackOperation> start() {
        return new Action<>(getTestContextStackOperation(), StackOperation::start);
    }

    public static Action<StackOperation> stop() {
        return new Action<>(getTestContextStackOperation(), StackOperation::stop);
    }

    public static Action<StackOperation> sync() {
        return new Action<>(getTestContextStackOperation(), StackOperation::sync);
    }

    public static Assertion<StackOperation> assertThis(BiConsumer<StackOperation, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextStackOperation(GherkinTest.RESULT), check);
    }
}
