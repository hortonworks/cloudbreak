package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import java.io.IOException;

import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ImageCatalogCreateIfNotExistsAction implements Action<ImageCatalogTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogCreateIfNotExistsAction.class);

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient client) throws IOException {
        Log.when(LOGGER, "Create Imagecatalog with name: " + testDto.getRequest().getName());
        try {
            testDto.setResponse(client.getDefaultClient(testContext).imageCatalogV4Endpoint().create(client.getWorkspaceId(), testDto.getRequest()));
            Log.whenJson(LOGGER, "Imagecatalog created successfully: ", testDto.getRequest());
        } catch (BadRequestException e) {
            Log.when(LOGGER, "Cannot create Imagecatalog, trying to fetch existed one: " + testDto.getRequest().getName());
            fetchIfExistsOrThrowOriginalException(testDto, client, e);
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("ImageCatalog could not be created.");
        }
        return testDto;
    }

    private static void fetchIfExistsOrThrowOriginalException(ImageCatalogTestDto testDto, CloudbreakClient client, BadRequestException originalException) {
        try {
            testDto.setResponse(client.getDefaultClient(testDto.getTestContext()).imageCatalogV4Endpoint()
                    .getByName(client.getWorkspaceId(), testDto.getRequest().getName(), Boolean.FALSE, Boolean.FALSE));
            Log.whenJson(LOGGER, "Imagecatalog fetched successfully: ", testDto.getRequest());
        } catch (Exception e) {
            throw originalException;
        }
    }
}
