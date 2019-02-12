package com.sequenceiq.it.cloudbreak.newway.entity;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;

import java.io.IOException;
import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ImageCatalogDto extends AbstractCloudbreakEntity<ImageCatalogV4Request, ImageCatalogV4Response, ImageCatalogDto>
        implements Purgable<ImageCatalogV4Response> {

    public static final String IMAGE_CATALOG = "IMAGE_CATALOG";

    public static final String IMAGE_CATALOG_URL = "IMAGE_CATALOG_URL";

    private ImagesV4Response imagesV4Response;

    public ImageCatalogDto(TestContext testContext) {
        super(new ImageCatalogV4Request(), testContext);
    }

    public ImageCatalogDto(ImageCatalogV4Request imageCatalogV4Request, TestContext testContext) {
        super(imageCatalogV4Request, testContext);
    }

    public ImageCatalogDto() {
        super(IMAGE_CATALOG);
        setRequest(new ImageCatalogV4Request());
    }

    public ImageCatalogDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ImageCatalogDto withUrl(String url) {
        getRequest().setUrl(url);
        return this;
    }

    public ImageCatalogDto valid() {
        MockedTestContext mockedTestContext = (MockedTestContext) getTestContext();
        return getCloudProvider().imageCatalog(withName(getNameCreator().getRandomNameForResource())
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getImageCatalogUrl()));
    }

    public ImagesV4Response getResponseByProvider() {
        return imagesV4Response;
    }

    public void setResponseByProvider(ImagesV4Response imagesV4Response) {
        this.imagesV4Response = imagesV4Response;
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        delete(context, getResponse(), cloudbreakClient);
    }

    public static ImageCatalogDto putSetDefaultByName(TestContext testContext, ImageCatalogDto entity, CloudbreakClient cloudbreakClient)
            throws IOException {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .imageCatalogV4Endpoint().setDefault(cloudbreakClient.getWorkspaceId(), entity.getRequest().getName()));
        logJSON(LOGGER, "ImageCatalog set to default: ", entity.getResponse());
        return entity;
    }

    @Override
    public Collection<ImageCatalogV4Response> getAll(CloudbreakClient client) {
        return client.getCloudbreakClient().imageCatalogV4Endpoint().list(client.getWorkspaceId()).getResponses();
    }

    @Override
    public boolean deletable(ImageCatalogV4Response entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(TestContext testContext, ImageCatalogV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().imageCatalogV4Endpoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), getErrorMessage(e), e);
        }
    }

    @Override
    public int order() {
        return 1000;
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }
}