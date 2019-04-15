package com.sequenceiq.it.cloudbreak.newway.action.v4.imagecatalog;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;

public class ImageCatalogCreateIfNotExistsAction implements Action<ImageCatalogTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogCreateIfNotExistsAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient client) {
        LOGGER.info("Create Imagecatalog with name: {}", testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    client.getCloudbreakClient().imageCatalogV4Endpoint().create(client.getWorkspaceId(), testDto.getRequest())
            );
            logJSON(LOGGER, "Imagecatalog created successfully: ", testDto.getRequest());
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
