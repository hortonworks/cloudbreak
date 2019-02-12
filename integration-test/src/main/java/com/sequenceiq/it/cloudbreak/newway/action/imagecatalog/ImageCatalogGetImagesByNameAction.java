package com.sequenceiq.it.cloudbreak.newway.action.imagecatalog;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ImageCatalogDto;

public class ImageCatalogGetImagesByNameAction implements Action<ImageCatalogDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogGetByNameAction.class);

    private CloudPlatform platform = CloudPlatform.MOCK;

    public ImageCatalogGetImagesByNameAction() {
    }

    public ImageCatalogGetImagesByNameAction(CloudPlatform platform) {
        this.platform = platform;
    }

    @Override
    public ImageCatalogDto action(TestContext testContext, ImageCatalogDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        LOGGER.info("Get images of ImageCatalog within workspace by catalog name: {}", entity.getRequest().getName());
        try {
            entity.setResponseByProvider(
                    cloudbreakClient
                            .getCloudbreakClient()
                            .imageCatalogV4Endpoint()
                            .getImagesByName(cloudbreakClient.getWorkspaceId(), entity.getName(), null, platform.name())
            );
            logJSON(LOGGER, "images have been fetched successfully: ", entity.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot get images of ImageCatalog : {}", entity.getRequest().getName());
            throw e;
        }
        return entity;
    }
}

