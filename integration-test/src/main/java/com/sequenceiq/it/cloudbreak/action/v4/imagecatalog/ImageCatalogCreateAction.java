package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ImageCatalogCreateAction implements Action<ImageCatalogTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogCreateAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, format(" Image catalog post request:%n"), testDto.getRequest());
        try {
            testDto.setResponse(
                    client.getDefaultClient(testContext)
                            .imageCatalogV4Endpoint()
                            .create(client.getWorkspaceId(), testDto.getRequest()));
        } catch (Exception e) {
            Log.when(LOGGER, "Unable to create image catalog!" + e);
            throw e;
        }
        Log.whenJson(LOGGER, format(" Image catalog created  successfully:%n"), testDto.getResponse());

        return testDto;
    }

}