package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.stack.StackImageChangeRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackImageChange extends AbstractCloudbreakEntity<StackImageChangeRequest, Response> {
    public static final String IMAGE_CHANGE = "IMAGE_CHANGE";

    protected StackImageChange(String newId) {
        super(newId);
        setRequest(new StackImageChangeRequest());
    }

    protected StackImageChange() {
        this(IMAGE_CHANGE);
    }

    public StackImageChange withImageId(String imageId) {
        getRequest().setImageId(imageId);
        return this;
    }

    public StackImageChange withImageCatalogName(String imageCatalogName) {
        getRequest().setImageCatalogName(imageCatalogName);
        return this;
    }

    public static void changeImage(IntegrationTestContext integrationTestContext, Entity entity) {
        StackImageChange stackImageChange = (StackImageChange) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" changeImage " + stack.getRequest().getGeneral().getName());
        stackImageChange.setResponse(client.getCloudbreakClient().stackV2Endpoint()
                .changeImage(stack.getRequest().getGeneral().getName(), stackImageChange.getRequest()));
    }

    public static StackImageChange request() {
        return new StackImageChange();
    }

    static Function<IntegrationTestContext, StackImageChange> getTestContextStackImageChange(String key) {
        return testContext -> testContext.getContextParam(key, StackImageChange.class);
    }

    static Function<IntegrationTestContext, StackImageChange> getTestContextStackImageChange() {
        return getTestContextStackImageChange(IMAGE_CHANGE);
    }

    public static Action<StackImageChange> changeImage() {
        return new Action<>(getTestContextStackImageChange(), StackImageChange::changeImage);
    }
}
