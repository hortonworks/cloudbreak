package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ImageCatalogGetImagesFromDefaultCatalogAction implements Action<ImageCatalogTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogGetImagesFromDefaultCatalogAction.class);

    private CloudPlatform platform = CloudPlatform.MOCK;

    public ImageCatalogGetImagesFromDefaultCatalogAction() {
    }

    public ImageCatalogGetImagesFromDefaultCatalogAction(CloudPlatform platform) {
        this.platform = platform;
    }

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        LOGGER.info("Get images of ImageCatalog within workspace by catalog name: {}", testDto.getRequest().getName());
        try {
            testDto.setResponseByProvider(
                    cloudbreakClient
                            .getCloudbreakClient()
                            .imageCatalogV4Endpoint()
                            .getImages(cloudbreakClient.getWorkspaceId(), null, platform.name())
            );
            Log.logJSON(LOGGER, "images have been fetched successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot get images of ImageCatalog : {}", testDto.getRequest().getName());
            throw e;
        }
        return testDto;
    }
}
