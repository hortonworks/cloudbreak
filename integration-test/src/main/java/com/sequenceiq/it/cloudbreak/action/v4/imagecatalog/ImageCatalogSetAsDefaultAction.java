package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ImageCatalogSetAsDefaultAction implements Action<ImageCatalogTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogSetAsDefaultAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient cloudbreakClient) {
        Log.when(LOGGER, "Set Imagecatalog as default within workspace with name: " + testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    cloudbreakClient.getCloudbreakClient().imageCatalogV4Endpoint().setDefault(cloudbreakClient.getWorkspaceId(), testDto.getName())
            );
            Log.whenJson(LOGGER, "Imagecatalog has been set as default successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot set Imagecatalog as default: {}", testDto.getRequest().getName());
            throw new IllegalStateException("ImageCatalog could not be set as default.", e);
        }
        return testDto;
    }
}
