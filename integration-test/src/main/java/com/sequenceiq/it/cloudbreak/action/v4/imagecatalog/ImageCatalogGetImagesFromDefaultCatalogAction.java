package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ImageCatalogGetImagesFromDefaultCatalogAction implements Action<ImageCatalogTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogGetImagesFromDefaultCatalogAction.class);

    private CloudPlatform platform = CloudPlatform.MOCK;

    public ImageCatalogGetImagesFromDefaultCatalogAction() {
    }

    public ImageCatalogGetImagesFromDefaultCatalogAction(CloudPlatform platform) {
        this.platform = platform;
    }

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.when(LOGGER, "Get Imagecatalog by platform: " + platform.name());
        try {
            testDto.setResponseByProvider(
                    cloudbreakClient
                            .getDefaultClient(testContext)
                            .imageCatalogV4Endpoint()
                            .getImages(cloudbreakClient.getWorkspaceId(), null, platform.name(), null, null, false, false, null)
            );
            Log.whenJson(LOGGER, "images have been fetched successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot get images of ImageCatalog : {}", testDto.getRequest().getName());
            throw e;
        }
        return testDto;
    }
}
