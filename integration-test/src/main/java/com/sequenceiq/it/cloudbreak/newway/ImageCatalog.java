package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.ImageCatalogV3Action;

@Prototype
public class ImageCatalog extends ImageCatalogEntity {

    public ImageCatalog(TestContext testContext) {
        super(new ImageCatalogRequest(), testContext);
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

    public static Action<ImageCatalog> post(String key) {
        return new Action<>(getTestContext(key), ImageCatalogV3Action::post);
    }

    public static Action<ImageCatalog> post() {
        return post(IMAGE_CATALOG);
    }

    public static Action<ImageCatalog> get(String key) {
        return new Action<>(getTestContext(key), ImageCatalogV3Action::get);
    }

    public static Action<ImageCatalog> get() {
        return get(IMAGE_CATALOG);
    }

    public static Action<ImageCatalog> getAll() {
        return new Action<>(getNew(), ImageCatalogV3Action::getAll);
    }

    public static Action<ImageCatalog> getImagesByProvider() {
        return new Action<>(getNew(), ImageCatalogV3Action::getImagesByProvider);
    }

    public static Action<ImageCatalog> getImagesByProviderFromImageCatalog(String key) {
        return new Action<>(getTestContext(key), ImageCatalogV3Action::getImagesByProviderFromImageCatalog);
    }

    public static Action<ImageCatalog> getImagesByProviderFromImageCatalog() {
        return getImagesByProviderFromImageCatalog(IMAGE_CATALOG);
    }

    public static Action<ImageCatalog> getRequestFromName(String key) {
        return new Action<>(getTestContext(key), ImageCatalogV3Action::getRequestByName);
    }

    public static Action<ImageCatalog> getRequestFromName() {
        return getRequestFromName(IMAGE_CATALOG);
    }

    public static Action<ImageCatalog> delete(String key) {
        return new Action<>(getTestContext(key), ImageCatalogV3Action::delete);
    }

    public static Action<ImageCatalog> delete() {
        return delete(IMAGE_CATALOG);
    }

    public static Assertion<ImageCatalog> assertThis(BiConsumer<ImageCatalog, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static Action<ImageCatalog> setDefault(String key) {
        return new Action<>(getTestContext(key), ImageCatalogV3Action::putSetDefaultByName);
    }

    public static Action<ImageCatalog> setDefault() {
        return setDefault(IMAGE_CATALOG);
    }
}