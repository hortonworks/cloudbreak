package com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;

public class ImageCatalogGetAction implements Action<ImageCatalogTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogGetAction.class);

    private boolean withImages = Boolean.FALSE;

    public ImageCatalogGetAction() {
    }

    public ImageCatalogGetAction(boolean withImages) {
        this.withImages = withImages;
    }

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        LOGGER.info("Get Imagecatalog within workspace by name: {}", testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    cloudbreakClient.getCloudbreakClient().imageCatalogV4Endpoint().get(cloudbreakClient.getWorkspaceId(), testDto.getName(), withImages)
            );
            logJSON(LOGGER, "Imagecatalog has been fetched successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot get Imagecatalog : {}", testDto.getRequest().getName());
            throw e;
        }
        return testDto;
    }
}
