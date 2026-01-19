package com.sequenceiq.it.cloudbreak.dto.imagecatalog;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class ImageCatalogTestDto extends AbstractCloudbreakTestDto<ImageCatalogV4Request, ImageCatalogV4Response, ImageCatalogTestDto>
        implements Purgable<ImageCatalogV4Response, CloudbreakClient> {

    private static final String IMAGECATALOG_RESOURCE_NAME = "imagecatalogName";

    private ImagesV4Response imagesV4Response;

    private Boolean skipCleanup = Boolean.FALSE;

    public ImageCatalogTestDto(TestContext testContext) {
        super(new ImageCatalogV4Request(), testContext);
    }

    public ImageCatalogTestDto(ImageCatalogV4Request imageCatalogV4Request, TestContext testContext) {
        super(imageCatalogV4Request, testContext);
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
    public void deleteForCleanup() {
        if (!skipCleanup) {
            getClientForCleanup().getDefaultClient(getTestContext()).imageCatalogV4Endpoint().deleteByCrn(0L, getCrn());
        }
    }

    @Override
    public Collection<ImageCatalogV4Response> getAll(CloudbreakClient client) {
        return client.getDefaultClient(getTestContext()).imageCatalogV4Endpoint().list(client.getWorkspaceId(), false).getResponses();
    }

    @Override
    public boolean deletable(ImageCatalogV4Response entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(TestContext testContext, ImageCatalogV4Response entity, CloudbreakClient client) {
        try {
            client.getDefaultClient(getTestContext()).imageCatalogV4Endpoint().deleteByName(client.getWorkspaceId(), entity.getName());
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