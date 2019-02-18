package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.ImageCatalogV3Action;

public class ImageCatalog extends ImageCatalogEntity {

    public ImageCatalog(TestContext testContext) {
        super(new ImageCatalogV4Request(), testContext);
    }

    public ImageCatalog() {
    }

    static Function<IntegrationTestContext, ImageCatalog> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, ImageCatalog.class);
    }

    static Function<IntegrationTestContext, ImageCatalog> getNew() {
        return testContext -> new ImageCatalog();
    }

    public static ImageCatalog request() {
        return new ImageCatalog();
    }

    public static ImageCatalog isCreated() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setCreationStrategy(ImageCatalogV3Action::createInGiven);
        return imageCatalog;
    }

    public static ImageCatalog isCreatedDeleted() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setCreationStrategy(ImageCatalogV3Action::createDeleteInGiven);
        return imageCatalog;
    }

    public static ImageCatalog isCreatedAsDefault() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setCreationStrategy(ImageCatalogV3Action::createAsDefaultInGiven);
        return imageCatalog;
    }

    public static ResourceAction post(String key) {
        return new ResourceAction(getTestContext(key), ImageCatalogV3Action::post);
    }

    public static ResourceAction post() {
        return post(IMAGE_CATALOG);
    }

    public static ResourceAction get(String key) {
        return new ResourceAction(getTestContext(key), ImageCatalogV3Action::get);
    }

    public static ResourceAction get() {
        return get(IMAGE_CATALOG);
    }

    public static ResourceAction getAll() {
        return new ResourceAction(getNew(), ImageCatalogV3Action::getAll);
    }

    public static ResourceAction getImagesByProvider() {
        return new ResourceAction(getNew(), ImageCatalogV3Action::getImagesByProvider);
    }

    public static ResourceAction getImagesByProviderFromImageCatalog(String key) {
        return new ResourceAction(getTestContext(key), ImageCatalogV3Action::getImagesByProviderFromImageCatalog);
    }

    public static ResourceAction getImagesByProviderFromImageCatalog() {
        return getImagesByProviderFromImageCatalog(IMAGE_CATALOG);
    }

    public static ResourceAction getRequestFromName(String key) {
        return new ResourceAction(getTestContext(key), ImageCatalogV3Action::getRequestByName);
    }

    public static ResourceAction getRequestFromName() {
        return getRequestFromName(IMAGE_CATALOG);
    }

    public static ResourceAction delete(String key) {
        return new ResourceAction(getTestContext(key), ImageCatalogV3Action::delete);
    }

    public static ResourceAction delete() {
        return delete(IMAGE_CATALOG);
    }

    public static Assertion<ImageCatalog> assertThis(BiConsumer<ImageCatalog, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static ResourceAction setDefault(String key) {
        return new ResourceAction(getTestContext(key), ImageCatalogV3Action::putSetDefaultByName);
    }

    public static ResourceAction setDefault() {
        return setDefault(IMAGE_CATALOG);
    }

    public static ImageCatalogEntity getByNameAndImages(TestContext testContext, ImageCatalogEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().imageCatalogV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName(), Boolean.TRUE)
        );
        return entity;
    }

    public static ImageCatalogEntity getByNameWithoutImages(TestContext testContext, ImageCatalogEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().imageCatalogV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName(), Boolean.FALSE)
        );
        return entity;
    }

    public static Action<ImageCatalogEntity> postV2() {
        return new ImageCatalogPostAction();
    }
}