package com.sequenceiq.it.cloudbreak.newway.v4;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ChangeImageAction implements Action<StackTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeImageAction.class);

    private StackImageChangeV4Request request = new StackImageChangeV4Request();

    public ChangeImageAction withImageId(String imageId) {
        request.setImageId(imageId);
        return this;
    }

    public static ChangeImageAction valid() {
        return new ChangeImageAction().withImageId("imageId");
    }

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto entity, CloudbreakClient client) throws Exception {
        ImageCatalogEntity imageCatalogEntity = testContext.get(ImageCatalogEntity.class);
        request.setImageCatalogName(imageCatalogEntity.getName());

        logJSON(" Enable Maintenance Mode post request:\n", request);

        client.getCloudbreakClient()
                .stackV4Endpoint()
                .changeImage(client.getWorkspaceId(), entity.getName(), request);

        return entity;
    }
}
