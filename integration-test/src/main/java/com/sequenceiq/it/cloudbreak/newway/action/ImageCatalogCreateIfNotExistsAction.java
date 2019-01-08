package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.filter.GetImageCatalogV4Filter;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ImageCatalogCreateIfNotExistsAction implements ActionV2<ImageCatalogEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogCreateIfNotExistsAction.class);

    @Override
    public ImageCatalogEntity action(TestContext testContext, ImageCatalogEntity entity, CloudbreakClient client) throws Exception {
        LOGGER.info("Create Imagecatalog with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    client.getCloudbreakClient().imageCatalogV4Endpoint().create(client.getWorkspaceId(), entity.getRequest())
            );
            logJSON(LOGGER, "Imagecatalog created successfully: ", entity.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create Imagecatalog, fetch existed one: {}", entity.getRequest().getName());
            GetImageCatalogV4Filter filter = new GetImageCatalogV4Filter();
            filter.setWithImages(false);
            entity.setResponse(
                    client.getCloudbreakClient().imageCatalogV4Endpoint()
                            .get(client.getWorkspaceId(), entity.getRequest().getName(), filter));
        }
        if (entity.getResponse() == null) {
            throw new IllegalStateException("ImageCatalog could not be created.");
        }
        return entity;
    }
}
