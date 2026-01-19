package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ImageCatalogGetAction implements Action<ImageCatalogTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogGetAction.class);

    private boolean withImages = Boolean.FALSE;

    private Boolean applyVersionBasedFiltering;

    public ImageCatalogGetAction() {
    }

    public ImageCatalogGetAction(boolean withImages) {
        this.withImages = withImages;
    }

    public ImageCatalogGetAction(boolean withImages, boolean applyVersionBasedFiltering) {
        this.withImages = withImages;
        this.applyVersionBasedFiltering = applyVersionBasedFiltering;
    }

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.when(LOGGER, "Get Imagecatalog by name: " + testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    cloudbreakClient.getDefaultClient(testContext).imageCatalogV4Endpoint()
                            .getByName(cloudbreakClient.getWorkspaceId(), testDto.getName(), withImages, applyVersionBasedFiltering)
            );
            Log.whenJson(LOGGER, "Imagecatalog has been fetched successfully: ", testDto.getResponse());
        } catch (Exception e) {
            LOGGER.warn("Cannot get Imagecatalog : {}", testDto.getRequest().getName());
            throw e;
        }
        return testDto;
    }
}
