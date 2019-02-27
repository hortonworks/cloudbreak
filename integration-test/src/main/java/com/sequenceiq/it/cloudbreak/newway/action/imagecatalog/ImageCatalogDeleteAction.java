package com.sequenceiq.it.cloudbreak.newway.action.imagecatalog;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.imagecatalog.ImageCatalogTestDto;

public class ImageCatalogDeleteAction implements Action<ImageCatalogTestDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogDeleteAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Image catalog DELETE request:%n"), entity.getRequest());
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .imageCatalogV4Endpoint()
                        .delete(cloudbreakClient.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, format(" Image catalog has been deleted successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }
}
