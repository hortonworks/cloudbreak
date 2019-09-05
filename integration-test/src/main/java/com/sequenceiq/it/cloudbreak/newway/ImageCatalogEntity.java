package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.io.IOException;
import java.util.Collection;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImagesResponse;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ImageCatalogEntity extends AbstractCloudbreakEntity<ImageCatalogRequest, ImageCatalogResponse, ImageCatalogEntity, ImageCatalogResponse> {
    public static final String IMAGE_CATALOG = "IMAGE_CATALOG";

    public static final String IMAGE_CATALOG_URL = "IMAGE_CATALOG_URL";

    private ImagesResponse imagesResponse;

    ImageCatalogEntity(ImageCatalogRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public ImageCatalogEntity(TestContext testContext) {
        super(new ImageCatalogRequest(), testContext);
    }

    public ImageCatalogEntity() {
        super(IMAGE_CATALOG);
        setRequest(new ImageCatalogRequest());
    }

    public ImageCatalogEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ImageCatalogEntity withUrl(String url) {
        getRequest().setUrl(url);
        return this;
    }

    public ImageCatalogEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withUrl(getTestContext().getImageCatalogMockServerSetup().getImageCatalogUrl());
    }

    public ImagesResponse getResponseByProvider() {
        return imagesResponse;
    }

    public void setResponseByProvider(ImagesResponse imagesResponse) {
        this.imagesResponse = imagesResponse;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        deleteV2(context, this, cloudbreakClient);
    }

    public static ImageCatalogEntity putSetDefaultByName(TestContext testContext, ImageCatalogEntity entity, CloudbreakClient cloudbreakClient)
            throws IOException {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .imageCatalogV3Endpoint().putSetDefaultByNameInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getRequest().getName()));
        logJSON(LOGGER, "ImageCatalog set to default: ", entity.getResponse());
        return entity;
    }

    public static ImageCatalogEntity deleteV2(TestContext testContext, ImageCatalogEntity entity, CloudbreakClient cloudbreakClient) {
        cloudbreakClient.getCloudbreakClient().imageCatalogV3Endpoint().deleteInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getName());
        return entity;
    }

    @Override
    public Collection<ImageCatalogResponse> getAll(CloudbreakClient client) {
        return client.getCloudbreakClient().imageCatalogV3Endpoint().listByWorkspace(client.getWorkspaceId());
    }

    @Override
    public boolean deletable(ImageCatalogResponse entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(ImageCatalogResponse entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().imageCatalogV3Endpoint().deleteInWorkspace(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), e.getMessage(), e);
        }
    }
}