package com.sequenceiq.it.cloudbreak.newway.action.imagecatalog;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.entity.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ImageCatalogCreateIfNotExistsAction implements Action<ImageCatalogTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogCreateIfNotExistsAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto entity, CloudbreakClient client) throws Exception {
        LOGGER.info("Create Imagecatalog with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    client.getCloudbreakClient().imageCatalogV4Endpoint().create(client.getWorkspaceId(), entity.getRequest())
            );
            logJSON(LOGGER, "Imagecatalog created successfully: ", entity.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create Imagecatalog, fetch existed one: {}", entity.getRequest().getName());
            entity.setResponse(
                    client.getCloudbreakClient().imageCatalogV4Endpoint()
                            .get(client.getWorkspaceId(), entity.getRequest().getName(), Boolean.FALSE));
        }
        if (entity.getResponse() == null) {
            throw new IllegalStateException("ImageCatalog could not be created.");
        }
        return entity;
    }
}
