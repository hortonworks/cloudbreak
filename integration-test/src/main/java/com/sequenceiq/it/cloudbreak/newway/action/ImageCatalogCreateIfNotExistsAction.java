package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ImageCatalogCreateIfNotExistsAction implements ActionV2<ImageCatalogEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogCreateIfNotExistsAction.class);

    @Override
    public ImageCatalogEntity action(TestContext testContext, ImageCatalogEntity entity, CloudbreakClient client) {
        LOGGER.info("Create Imagecatalog with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    client.getCloudbreakClient().imageCatalogV3Endpoint().createInWorkspace(client.getWorkspaceId(), entity.getRequest())
            );
            logJSON(LOGGER, "Imagecatalog created successfully: ", entity.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create Imagecatalog, fetch existed one: {}", entity.getRequest().getName());
            entity.setResponse(
                    client.getCloudbreakClient().imageCatalogV3Endpoint()
                            .getByNameInWorkspace(client.getWorkspaceId(), entity.getRequest().getName(), false));
        }
        if (entity.getResponse() == null) {
            throw new IllegalStateException("ImageCatalog could not be created.");
        }
        return entity;
    }
}
