package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.stack.StackImageChangeRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackImageChangeEntity extends AbstractCloudbreakEntity<StackImageChangeRequest, Response, StackImageChangeEntity, Response> {
    public static final String IMAGE_CHANGE = "IMAGE_CHANGE";

    protected StackImageChangeEntity(String newId) {
        super(newId);
        setRequest(new StackImageChangeRequest());
    }

    protected StackImageChangeEntity() {
        this(IMAGE_CHANGE);
    }

    public StackImageChangeEntity withImageId(String imageId) {
        getRequest().setImageId(imageId);
        return this;
    }

    public StackImageChangeEntity withImageCatalogName(String imageCatalogName) {
        getRequest().setImageCatalogName(imageCatalogName);
        return this;
    }

    public static void changeImage(IntegrationTestContext integrationTestContext, Entity entity) {
        StackImageChangeEntity stackImageChange = (StackImageChangeEntity) entity;
        CloudbreakClient client;
        client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);
        StackEntity stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" changeImage " + stack.getRequest().getGeneral().getName());
        stackImageChange.setResponse(client.getCloudbreakClient().stackV3Endpoint()
                .changeImage(client.getWorkspaceId(), stack.getRequest().getGeneral().getName(), stackImageChange.getRequest()));
    }

    public static StackImageChangeEntity request() {
        return new StackImageChangeEntity();
    }

    static Function<IntegrationTestContext, StackImageChangeEntity> getTestContextStackImageChange(String key) {
        return testContext -> testContext.getContextParam(key, StackImageChangeEntity.class);
    }

    static Function<IntegrationTestContext, StackImageChangeEntity> getTestContextStackImageChange() {
        return getTestContextStackImageChange(IMAGE_CHANGE);
    }

    public static Action<StackImageChangeEntity> changeImage() {
        return new Action<>(getTestContextStackImageChange(), StackImageChangeEntity::changeImage);
    }
}
