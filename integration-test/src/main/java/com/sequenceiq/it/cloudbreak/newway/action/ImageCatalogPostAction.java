package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ImageCatalogPostAction implements Action<ImageCatalogEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogPostAction.class);

    @Override
    public ImageCatalogEntity action(TestContext testContext, ImageCatalogEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Image catalog post request:%n"), entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogV4Endpoint()
                        .create(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, format(" Image catalog created  successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

}