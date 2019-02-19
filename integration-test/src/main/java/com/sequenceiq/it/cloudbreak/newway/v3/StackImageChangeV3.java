package com.sequenceiq.it.cloudbreak.newway.v3;

import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.ResourceAction;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackImageChangeV3 extends AbstractCloudbreakEntity<StackImageChangeV4Request, Response, StackImageChangeV3> {
    public static final String IMAGE_CHANGE = "IMAGE_CHANGE";

    protected StackImageChangeV3(String newId) {
        super(newId);
        setRequest(new StackImageChangeV4Request());
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
        StackTestDto stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" changeImage " + stack.getRequest().getName());
        client.getCloudbreakClient().stackV4Endpoint().changeImage(workspaceId, stack.getRequest().getName(), stackImageChange.getRequest());
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

    public static ResourceAction<StackImageChangeV3> changeImage() {
        return new ResourceAction<>(getTestContextStackImageChange(), StackImageChangeV3::changeImage);
    }
}
