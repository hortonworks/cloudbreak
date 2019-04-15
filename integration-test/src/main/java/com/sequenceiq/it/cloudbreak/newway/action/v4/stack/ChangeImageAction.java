package com.sequenceiq.it.cloudbreak.newway.action.v4.stack;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

public class ChangeImageAction implements Action<StackTestDto> {

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

        logJSON(" Enable Maintenance Mode post request:\n", request);

        client.getCloudbreakClient()
                .stackV4Endpoint()
                .changeImage(client.getWorkspaceId(), testDto.getName(), request);

        return testDto;
    }
}
