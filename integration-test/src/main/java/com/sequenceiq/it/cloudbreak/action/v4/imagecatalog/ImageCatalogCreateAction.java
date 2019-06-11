package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ImageCatalogCreateAction implements Action<ImageCatalogTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogCreateAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, format(" Image catalog post request:%n"), testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .imageCatalogV4Endpoint()
                        .create(client.getWorkspaceId(), testDto.getRequest()));
        Log.logJSON(LOGGER, format(" Image catalog created  successfully:%n"), testDto.getResponse());
        Log.log(LOGGER, format(" ID: %s", testDto.getResponse().getId()));

        return testDto;
    }

}