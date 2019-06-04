package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ChangeImageAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeImageAction.class);

    private StackImageChangeV4Request request = new StackImageChangeV4Request();

    public ChangeImageAction() {
        request.setImageId("imageId");
    }

    public ChangeImageAction withImageId(String imageId) {
        request.setImageId(imageId);
        return this;
    }

    public ChangeImageAction valid() {
        return new ChangeImageAction().withImageId("imageId");
    }

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        ImageCatalogTestDto imageCatalogTestDto = testContext.get(ImageCatalogTestDto.class);
        request.setImageCatalogName(imageCatalogTestDto.getName());

        Log.logJSON(" Enable Maintenance Mode post request:\n", request);

        client.getCloudbreakClient()
                .stackV4Endpoint()
                .changeImage(client.getWorkspaceId(), testDto.getName(), request);

        return testDto;
    }
}
