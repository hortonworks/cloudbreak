package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ImageCatalogGetAction implements Action<ImageCatalogTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogGetAction.class);

    private boolean withImages = Boolean.FALSE;

    public ImageCatalogGetAction() {
    }

    public ImageCatalogGetAction(boolean withImages) {
        this.withImages = withImages;
    }

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.when(LOGGER, "Get Imagecatalog by name: " + testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    cloudbreakClient.getDefaultClient().imageCatalogV4Endpoint().getByName(cloudbreakClient.getWorkspaceId(), testDto.getName(), withImages)
            );
            Log.whenJson(LOGGER, "Imagecatalog has been fetched successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot get Imagecatalog : {}", testDto.getRequest().getName());
            throw e;
        }
        return testDto;
    }
}
