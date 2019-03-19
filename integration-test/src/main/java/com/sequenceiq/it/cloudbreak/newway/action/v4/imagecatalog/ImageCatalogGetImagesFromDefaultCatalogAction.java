package com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;

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
            logJSON(LOGGER, "images have been fetched successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot get images of ImageCatalog : {}", testDto.getRequest().getName());
            throw e;
        }
        return testDto;
    }
}
