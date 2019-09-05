package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackOperationEntity extends AbstractCloudbreakEntity<StackScaleRequestV2, Response, StackOperationEntity, Response> {
    public static final String SCALE = "SCALE";

    protected StackOperationEntity(String newId) {
        super(newId);
        setRequest(new StackScaleRequestV2());
    }

    protected StackOperationEntity() {
        this(SCALE);
    }

    public StackOperationEntity withDesiredCount(int desiredCount) {
        getRequest().setDesiredCount(desiredCount);
        return this;
    }

    public StackOperationEntity withGroupName(String groupName) {
        getRequest().setGroup(groupName);
        return this;
    }

    public StackOperationEntity withStackId(Long x) {
        getRequest().setStackId(x);
        return this;
    }

    static Function<IntegrationTestContext, StackOperationEntity> getTestContextStackOperation(String key) {
        return testContext -> testContext.getContextParam(key, StackOperationEntity.class);
    }

    static Function<IntegrationTestContext, StackOperationEntity> getTestContextStackOperation() {
        return getTestContextStackOperation(SCALE);
    }

    public static void scale(IntegrationTestContext integrationTestContext, Entity entity) {
        StackOperationEntity stackOperation = (StackOperationEntity) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" scale " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV3Endpoint()
                        .putScalingInWorkspace(client.getWorkspaceId(), stack.getRequest().getGeneral().getName(), stackOperation.getRequest()));
    }

    public static void start(IntegrationTestContext integrationTestContext, Entity entity) {
        StackOperationEntity stackOperation = (StackOperationEntity) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" start " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV3Endpoint()
                        .putStartInWorkspace(client.getWorkspaceId(), stack.getRequest().getGeneral().getName()));
    }

    public static void stop(IntegrationTestContext integrationTestContext, Entity entity) {
        StackOperationEntity stackOperation = (StackOperationEntity) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" stop " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV3Endpoint()
                        .putStopInWorkspace(client.getWorkspaceId(), stack.getRequest().getGeneral().getName()));
    }

    public static void sync(IntegrationTestContext integrationTestContext, Entity entity) {
        StackOperationEntity stackOperation = (StackOperationEntity) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" sync " + stack.getRequest().getGeneral().getName());
        stackOperation.setResponse(
                client.getCloudbreakClient().stackV3Endpoint()
                        .putSyncInWorkspace(client.getWorkspaceId(), stack.getRequest().getGeneral().getName()));
    }

    public static StackOperationEntity request() {
        return new StackOperationEntity();
    }

    public static Action<StackOperationEntity> scale() {
        return new Action<>(getTestContextStackOperation(), StackOperationEntity::scale);
    }

    public static Action<StackOperationEntity> start() {
        return new Action<>(getTestContextStackOperation(), StackOperationEntity::start);
    }

    public static Action<StackOperationEntity> stop() {
        return new Action<>(getTestContextStackOperation(), StackOperationEntity::stop);
    }

    public static Action<StackOperationEntity> sync() {
        return new Action<>(getTestContextStackOperation(), StackOperationEntity::sync);
    }

    public static Assertion<StackOperationEntity> assertThis(BiConsumer<StackOperationEntity, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextStackOperation(GherkinTest.RESULT), check);
    }
}
