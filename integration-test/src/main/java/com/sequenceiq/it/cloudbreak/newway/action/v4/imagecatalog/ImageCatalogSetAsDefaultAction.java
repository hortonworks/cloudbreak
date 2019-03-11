package com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.imagecatalog.ImageCatalogTestDto;

public class ImageCatalogSetAsDefaultAction implements Action<ImageCatalogTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogSetAsDefaultAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto entity, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Set Imagecatalog as default within workspace with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    cloudbreakClient.getCloudbreakClient().imageCatalogV4Endpoint().setDefault(cloudbreakClient.getWorkspaceId(), entity.getName())
            );
            logJSON(LOGGER, "Imagecatalog has been set as default successfully: ", entity.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot set Imagecatalog as default: {}", entity.getRequest().getName());
            throw new IllegalStateException("ImageCatalog could not be set as default.", e);
        }
        return entity;
    }
}
