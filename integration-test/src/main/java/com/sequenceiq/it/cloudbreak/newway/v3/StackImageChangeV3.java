package com.sequenceiq.it.cloudbreak.newway.v3;

import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.stack.StackImageChangeRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Action;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackImageChangeV3 extends AbstractCloudbreakEntity<StackImageChangeRequest, Response, StackImageChangeV3> {
    public static final String IMAGE_CHANGE = "IMAGE_CHANGE";

    protected StackImageChangeV3(String newId) {
        super(newId);
        setRequest(new StackImageChangeRequest());
    }

    protected StackImageChangeV3() {
        this(IMAGE_CHANGE);
    }

    public StackImageChangeV3 withImageId(String imageId) {
        getRequest().setImageId(imageId);
        return this;
    }

    public StackImageChangeV3 withImageCatalogName(String imageCatalogName) {
        getRequest().setImageCatalogName(imageCatalogName);
        return this;
    }

    public static void changeImage(IntegrationTestContext integrationTestContext, Entity entity) {
        StackImageChangeV3 stackImageChange = (StackImageChangeV3) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" changeImage " + stack.getRequest().getGeneral().getName());
        stackImageChange.setResponse(client.getCloudbreakClient().stackV3Endpoint()
                .changeImage(workspaceId, stack.getRequest().getGeneral().getName(), stackImageChange.getRequest()));
    }

    public static StackImageChangeV3 request() {
        return new StackImageChangeV3();
    }

    static Function<IntegrationTestContext, StackImageChangeV3> getTestContextStackImageChange(String key) {
        return testContext -> testContext.getContextParam(key, StackImageChangeV3.class);
    }

    static Function<IntegrationTestContext, StackImageChangeV3> getTestContextStackImageChange() {
        return getTestContextStackImageChange(IMAGE_CHANGE);
    }

    public static Action<StackImageChangeV3> changeImage() {
        return new Action<>(getTestContextStackImageChange(), StackImageChangeV3::changeImage);
    }
}
