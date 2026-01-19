package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ImageCatalogDeleteAction implements Action<ImageCatalogTestDto, CloudbreakClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogDeleteAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.when(LOGGER, format(" Image catalog DELETE request:%n", testDto.getRequest().getName()));
        testDto.setResponse(
                cloudbreakClient.getDefaultClient(testContext)
                        .imageCatalogV4Endpoint()
                        .deleteByName(cloudbreakClient.getWorkspaceId(), testDto.getName()));
        Log.whenJson(LOGGER, format(" Image catalog has been deleted successfully:%n"), testDto.getResponse());

        return testDto;
    }
}
