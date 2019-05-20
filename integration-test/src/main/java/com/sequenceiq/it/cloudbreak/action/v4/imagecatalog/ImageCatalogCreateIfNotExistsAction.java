package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ImageCatalogCreateIfNotExistsAction implements Action<ImageCatalogTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogCreateIfNotExistsAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient client) {
        LOGGER.info("Create Imagecatalog with name: {}", testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    client.getCloudbreakClient().imageCatalogV4Endpoint().create(client.getWorkspaceId(), testDto.getRequest())
            );
            Log.logJSON(LOGGER, "Imagecatalog created successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create Imagecatalog, fetch existed one: {}", testDto.getRequest().getName());
            testDto.setResponse(
                    client.getCloudbreakClient().imageCatalogV4Endpoint()
                            .get(client.getWorkspaceId(), testDto.getRequest().getName(), Boolean.FALSE));
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("ImageCatalog could not be created.");
        }
        return testDto;
    }
}
