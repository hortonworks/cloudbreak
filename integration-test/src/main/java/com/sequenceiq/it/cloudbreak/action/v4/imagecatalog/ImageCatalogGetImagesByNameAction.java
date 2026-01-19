package com.sequenceiq.it.cloudbreak.action.v4.imagecatalog;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ImageCatalogGetImagesByNameAction implements Action<ImageCatalogTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogGetImagesByNameAction.class);

    private CloudPlatform platform = CloudPlatform.MOCK;

    private String stackName;

    private boolean defaultOnly;

    public ImageCatalogGetImagesByNameAction() {
    }

    public ImageCatalogGetImagesByNameAction(CloudPlatform platform) {
        this.platform = platform;
    }

    public ImageCatalogGetImagesByNameAction(CloudPlatform platform, boolean defaultOnly) {
        this.platform = platform;
        this.defaultOnly = defaultOnly;
    }

    public ImageCatalogGetImagesByNameAction(String stackName) {
        this.stackName = stackName;
    }

    @Override
    public ImageCatalogTestDto action(TestContext testContext, ImageCatalogTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Log.when(LOGGER, "Get Imagecatalog by name: " + testDto.getRequest().getName());
        try {
            ImageCatalogV4Endpoint imageCatalogV4Endpoint = cloudbreakClient
                    .getDefaultClient(testContext)
                    .imageCatalogV4Endpoint();

            testDto.setResponseByProvider(getImagesV4Response(testDto, cloudbreakClient, imageCatalogV4Endpoint));
            Log.whenJson(LOGGER, "images have been fetched successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot get images of ImageCatalog : {}", testDto.getRequest().getName());
            throw e;
        }
        return testDto;
    }

    private ImagesV4Response getImagesV4Response(ImageCatalogTestDto entity, CloudbreakClient cloudbreakClient, ImageCatalogV4Endpoint imageCatalogV4Endpoint)
            throws Exception {
        return StringUtils.isNotEmpty(stackName)
                ? imageCatalogV4Endpoint.getImagesByName(cloudbreakClient.getWorkspaceId(), entity.getName(), stackName, null, null,
                null, false, defaultOnly, null)
                : imageCatalogV4Endpoint.getImagesByName(cloudbreakClient.getWorkspaceId(), entity.getName(), null, platform.name(),
                null, null, false, defaultOnly, null);
    }
}

