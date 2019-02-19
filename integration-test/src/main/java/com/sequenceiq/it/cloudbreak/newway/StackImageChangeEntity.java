package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackImageChangeEntity extends AbstractCloudbreakEntity<StackImageChangeV4Request, Response, StackImageChangeEntity> {
    public static final String IMAGE_CHANGE = "IMAGE_CHANGE";

    protected StackImageChangeEntity(String newId) {
        super(newId);
        setRequest(new StackImageChangeV4Request());
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
        StackTestDto stack;
        stack = Stack.getTestContextStack().apply(integrationTestContext);
        Log.log(" changeImage " + stack.getRequest().getName());
        client.getCloudbreakClient().stackV4Endpoint().changeImage(client.getWorkspaceId(), stack.getRequest().getName(), stackImageChange.getRequest());
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

    public static ResourceAction<StackImageChangeEntity> changeImage() {
        return new ResourceAction<>(getTestContextStackImageChange(), StackImageChangeEntity::changeImage);
    }
}
