package com.sequenceiq.it.cloudbreak.dto.imagecatalog;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Collection;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class ImageCatalogTestDto extends AbstractCloudbreakTestDto<ImageCatalogV4Request, ImageCatalogV4Response, ImageCatalogTestDto>
        implements Purgable<ImageCatalogV4Response, CloudbreakClient> {

    public static final String IMAGE_CATALOG = "IMAGE_CATALOG";

    public static final String IMAGE_CATALOG_URL = "IMAGE_CATALOG_URL";

    private static final String IMAGECATALOG_RESOURCE_NAME = "imagecatalogName";

    private ImagesV4Response imagesV4Response;

    private Boolean skipCleanup = Boolean.FALSE;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    public ImageCatalogTestDto(TestContext testContext) {
        super(new ImageCatalogV4Request(), testContext);
    }

    public ImageCatalogTestDto(ImageCatalogV4Request imageCatalogV4Request, TestContext testContext) {
        super(imageCatalogV4Request, testContext);
    }

    public ImageCatalogTestDto() {
        super(IMAGE_CATALOG);
        setRequest(new ImageCatalogV4Request());
    }

    public ImageCatalogTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    @Override
    public String getResourceNameType() {
        return IMAGECATALOG_RESOURCE_NAME;
    }

    public ImageCatalogTestDto withUrl(String url) {
        getRequest().setUrl(url);
        return this;
    }

    public ImageCatalogTestDto withoutCleanup() {
        setSkipCleanup(Boolean.TRUE);
        return this;
    }

    public ImageCatalogTestDto valid() {
        return getCloudProvider().imageCatalog(withName(getResourcePropertyProvider().getName(getCloudPlatform())));
    }

    public ImagesV4Response getResponseByProvider() {
        return imagesV4Response;
    }

    public void setResponseByProvider(ImagesV4Response imagesV4Response) {
        this.imagesV4Response = imagesV4Response;
    }

    public void setSkipCleanup(Boolean skipCleanup) {
        this.skipCleanup = skipCleanup;
    }

    @Override
    public void cleanUp(TestContext context, MicroserviceClient client) {
        if (!skipCleanup) {
            LOGGER.info("Cleaning up image catalog with name: {}", getName());
            if (getResponse() != null) {
                when(imageCatalogTestClient.deleteV4(), key("delete-imagecatalog-" + getName()).withSkipOnFail(false));
            } else {
                LOGGER.info("Image catalog: {} response is null!", getName());
            }
        }
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
            client.getCloudbreakClient().imageCatalogV4Endpoint().deleteByName(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }

    @Override
    public Class<CloudbreakClient> client() {
        return CloudbreakClient.class;
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